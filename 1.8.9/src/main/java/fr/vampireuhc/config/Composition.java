package fr.vampireuhc.config;

import fr.vampireuhc.player.Camp;
import fr.vampireuhc.roles.RoleType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Composition d'une partie définie dans le config.yml (section "compo") :
 * nombre de joueurs requis + nombre de joueurs par rôle, par camp.
 *
 * Règle d'unicité : tous les rôles sont uniques (0 ou 1) SAUF VAMPIRE_MINION
 * (les Sbires peuvent être plusieurs).
 *
 * Une composition invalide (rôle inconnu, rôle non-Sbire compté plus d'une fois,
 * joueurs != somme des rôles) doit empêcher le lancement de la partie.
 */
public class Composition {

    private final int requiredPlayers;
    private final boolean valid;
    private final List<String> errors;

    // Camp -> rôle -> nombre attendu (insertion order = ordre du config.yml).
    private final Map<Camp, Map<RoleType, Integer>> rolesByCamp;

    private Composition(int requiredPlayers, List<String> errors,
                        Map<Camp, Map<RoleType, Integer>> rolesByCamp) {
        this.requiredPlayers = requiredPlayers;
        this.errors = errors;
        this.rolesByCamp = rolesByCamp;
        this.valid = errors.isEmpty();
    }

    public static Composition load(FileConfiguration cfg) {
        List<String> errors = new ArrayList<>();
        Map<Camp, Map<RoleType, Integer>> rolesByCamp = new EnumMap<>(Camp.class);

        parseCampSection(cfg, "vampires", Camp.VAMPIRE, rolesByCamp, errors);
        parseCampSection(cfg, "villageois", Camp.VILLAGEOIS, rolesByCamp, errors);
        parseCampSection(cfg, "solitaires", Camp.SOLO, rolesByCamp, errors);

        int requiredPlayers = cfg.getInt("compo.joueurs", -1);
        if (requiredPlayers < 0) {
            errors.add("compo.joueurs manquant (nombre de joueurs requis).");
        }

        int sum = 0;
        for (Map<RoleType, Integer> roles : rolesByCamp.values()) {
            for (int count : roles.values()) {
                sum += count;
            }
        }
        if (requiredPlayers >= 0 && requiredPlayers != sum) {
            errors.add("compo.joueurs (" + requiredPlayers + ") != somme des rôles (" + sum + ").");
        }

        return new Composition(requiredPlayers, errors, rolesByCamp);
    }

    // Parse une section "compo.<sectionKey>" et remplit rolesByCamp.
    private static void parseCampSection(FileConfiguration cfg, String sectionKey, Camp camp,
                                         Map<Camp, Map<RoleType, Integer>> rolesByCamp,
                                         List<String> errors) {
        Map<RoleType, Integer> roles = new LinkedHashMap<>();
        ConfigurationSection section = cfg.getConfigurationSection("compo." + sectionKey);
        if (section != null) {
            for (String key : section.getKeys(false)) {
                RoleType type = parseRoleType(key, sectionKey, errors);
                if (type == null) {
                    continue;
                }
                int count = parseInt(key, sectionKey, section.get(key), errors);
                if (count < 0) {
                    errors.add("compo." + sectionKey + "." + key + " : compteur négatif invalide.");
                    continue;
                }
                if (count > 1 && type != RoleType.VAMPIRE_MINION) {
                    errors.add("compo." + sectionKey + "." + key + " : ce rôle est unique (0 ou 1), seul VAMPIRE_MINION est répétable.");
                    continue;
                }
                roles.put(type, count);
            }
        }
        rolesByCamp.put(camp, roles);
    }

    private static RoleType parseRoleType(String key, String sectionKey, List<String> errors) {
        try {
            return RoleType.valueOf(key.toUpperCase());
        } catch (IllegalArgumentException e) {
            errors.add("compo." + sectionKey + "." + key + " : rôle inconnu.");
            return null;
        }
    }

    private static int parseInt(String key, String sectionKey, Object value, List<String> errors) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            errors.add("compo." + sectionKey + "." + key + " : valeur non numérique.");
            return -1;
        }
    }

    public int getRequiredPlayers() {
        return requiredPlayers;
    }

    public boolean isValid() {
        return valid;
    }

    public List<String> getErrors() {
        return errors;
    }

    // Rôles attendus pour un camp (insertion order), sans filtre des comptes à 0.
    public Map<RoleType, Integer> getRolesForCamp(Camp camp) {
        return rolesByCamp.get(camp);
    }

    // Nombre total de rôles à distribuer (== requiredPlayers si composi valide).
    public int getTotalRoles() {
        int sum = 0;
        for (Map<RoleType, Integer> roles : rolesByCamp.values()) {
            for (int count : roles.values()) {
                sum += count;
            }
        }
        return sum;
    }

    /**
     * Liste ordonnée des rôles à distribuer : tous les rôles uniques (dans
     * l'ordre vampires -> villageois -> solitaires), puis les Sbires à la fin.
     * En cas de joueurs absents, les premiers rôles non attribués sont donc
     * des Sbires (rôle répétable, impact minimal).
     */
    public List<RoleType> buildRoleList() {
        List<RoleType> roles = new ArrayList<>();
        int minions = 0;
        for (Camp camp : new Camp[] {Camp.VAMPIRE, Camp.VILLAGEOIS, Camp.SOLO}) {
            Map<RoleType, Integer> byCamp = rolesByCamp.get(camp);
            if (byCamp == null) {
                continue;
            }
            for (Map.Entry<RoleType, Integer> entry : byCamp.entrySet()) {
                if (entry.getKey() == RoleType.VAMPIRE_MINION) {
                    minions += entry.getValue();
                } else {
                    for (int i = 0; i < entry.getValue(); i++) {
                        roles.add(entry.getKey());
                    }
                }
            }
        }
        for (int i = 0; i < minions; i++) {
            roles.add(RoleType.VAMPIRE_MINION);
        }
        return roles;
    }

    // Le camp d'un rôle (tel que défini par la composition).
    public static Camp campOf(RoleType type) {
        switch (type) {
            case MASTER:
            case COMTE:
            case VAMPIRE_MINION:
                return Camp.VAMPIRE;
            case APPRENTICE_SLAYER:
            case GREMLIN:
            case DOPPELGANGER:
                return Camp.SOLO;
            default:
                return Camp.VILLAGEOIS;
        }
    }
}