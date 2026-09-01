package fr.vampireuhc.roles;

import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.config.MessageUtil;
import fr.vampireuhc.player.Camp;
import fr.vampireuhc.player.PlayerManager;
import fr.vampireuhc.player.VampireUHCPlayer;
import fr.vampireuhc.roles.usurped.UsurpedPower;
import fr.vampireuhc.roles.usurped.UsurpedPriest;
import fr.vampireuhc.roles.usurped.UsurpedSoulweigher;
import fr.vampireuhc.roles.usurped.UsurpedMaster;
import fr.vampireuhc.roles.usurped.UsurpedVampire;
import fr.vampireuhc.roles.usurped.UsurpedComte;
import fr.vampireuhc.roles.usurped.UsurpedBanshee;
import fr.vampireuhc.roles.usurped.UsurpedGravedigger;
import fr.vampireuhc.roles.usurped.UsurpedExorcist;
import fr.vampireuhc.roles.usurped.UsurpedGremlin;
import fr.vampireuhc.roles.usurped.UsurpedPaladin;
import fr.vampireuhc.roles.usurped.UsurpedSlayer;
import fr.vampireuhc.roles.usurped.UsurpedSavior;
import fr.vampireuhc.roles.usurped.UsurpedArcher;
import fr.vampireuhc.roles.usurped.UsurpedBourreau;
import fr.vampireuhc.roles.usurped.UsurpedCartographer;
import fr.vampireuhc.roles.usurped.UsurpedWatchman;
import fr.vampireuhc.roles.usurped.UsurpedCupidon;
import fr.vampireuhc.roles.usurped.UsurpedWeaver;
import fr.vampireuhc.roles.usurped.UsurpedSandMerchant;
import fr.vampireuhc.roles.usurped.UsurpedBabaYaga;
import fr.vampireuhc.roles.usurped.UsurpedWhiteLady;

import com.google.gson.JsonObject;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * Doppelganger (Sosie) : rôle solitaire qui copie, une seule fois entre
 * 20 et 60 minutes de jeu, les pouvoirs actifs et passifs d'un joueur
 * vivant, et ce jusqu'à la mort de ce dernier.
 *
 * Ce rôle possède SA PROPRE souche de marqueurs (variantes DOPPELGANGER,
 * source = le Sosie) et n'hérite jamais de l'état accumulé par la cible :
 * chaque pouvoir copié embarque ses propres compteurs, nets et gates.
 *
 * Règles (doc/Doppelganger.md) :
 *  - la cible doit être vivante et connectée, jamais un autre Sosie ;
 *  - mort de l'usurpé : perte immédiate des pouvoirs et des marqueurs posés
 *    par le Sosie, apprentissage de l'identité du tueur (si identifié, et
 *    s'il ne s'agit pas de lui) et +50% de dégâts permanents contre ce tueur ;
 *  - si le Sosie tue ensuite ce tueur : force + speed permanents ;
 *  - pas de tueur (chute, lave...) ou auto-kill : aucun bonus.
 */
public class DoppelgangerRole implements Role {

    private VampireUHCPlayer player;

    private boolean used = false;
    private VampireUHCPlayer usurped;       // joueur copié (null = aucun)
    private UsurpedPower activePower;       // pouvoir copié (null = aucun)

    // Mort de l'usurpé : identité du tueur + récompenses.
    private UUID killer;                    // tueur de l'usurpé (null = aucun / en attente)
    private boolean killerDamageBonus = false; // +50% de dégâts permanent contre le tueur
    private boolean killerKilled = false;      // tueur tué par le Sosie → force + speed permanents

    public DoppelgangerRole(VampireUHCPlayer player) {
        this.player = player;
    }

    @Override
    public Camp getDefaultCamp() {
        return Camp.SOLO;
    }

    @Override
    public String getName() {
        return "Doppelganger";
    }

    @Override
    public boolean isVampire() {
        return false;
    }

    @Override
    public void onAssign(VampireUHCPlayer player) {
        this.player = player;
    }

    @Override
    public String getDescription() {
        return (
            "<light_purple>Vous gagnez seul en éliminant tous les autres joueurs.</light_purple>\n\n"
            + "<dark_purple>▸</dark_purple> <gray>Entre <yellow>20</yellow> et <yellow>60 minutes</yellow>, vous pouvez copier "
            + "<red>une seule fois</red> l'intégralité des pouvoirs d'un joueur : <gold>/vuhc usurper <joueur></gold></gray>\n"
            + "<dark_purple>▸</dark_purple> <gray>Vous copiez le <yellow>pouvoir</yellow>, jamais l'état de la cible "
            + "(aucune information déjà accumulée, compteurs indépendants).</gray>\n\n"
            + "<bold><dark_purple>À la mort du joueur copié :</dark_purple></bold>\n"
            + "  <gray>• Vous perdez immédiatement ses pouvoirs et les marqueurs que vous avez posés en tant que ce rôle.</gray>\n"
            + "  <gray>• Vous apprenez l'identité de son tueur.</gray>\n"
            + "  <gray>• Vos dégâts contre ce tueur augmentent de <red>50%</red> de façon permanente.</gray>\n"
            + "  <gray>• Si vous parvenez à le tuer : <green>force + speed</green> permanents.</gray>"
        );
    }

    // --- Usurpation ---

    public boolean canUsurp(long elapsedSeconds) {
        if (used) {
            return false;
        }
        long start = VampireUHC.getInstance().getConfigManager().getUsurpWindowStartMin() * 60L;
        long end = VampireUHC.getInstance().getConfigManager().getUsurpWindowEndMin() * 60L;
        return elapsedSeconds >= start && elapsedSeconds <= end;
    }

    /**
     * Tente d'usurper {@code target}. Applique les gardes (fenêtre déjà
     * vérifiée par canUsurp, cible vivante/connectée/pas un Sosie) puis
     * construit le pouvoir copié. Renvoie le pouvoir copié, ou null si
     * aucun pouvoir n'est copiable pour l'instant.
     */
    public UsurpedPower usurp(VampireUHCPlayer target) {
        if (used || target == null) {
            return null;
        }
        UsurpedPower power = buildPower(target);
        if (power == null) {
            return null;
        }
        this.used = true;
        this.usurped = target;
        this.activePower = power;
        power.onEnter(player);
        return power;
    }

    // Fabrique des pouvoirs copiés — s'enrichit lot par lot.
    protected UsurpedPower buildPower(VampireUHCPlayer target) {
        if (target == null || target.getRole() == null) {
            return null;
        }
        Role role = target.getRole();
        if (role instanceof PriestRole) {
            return new UsurpedPriest();
        }
        if (role instanceof SoulweigherRole) {
            return new UsurpedSoulweigher();
        }
        if (role instanceof MasterRole) {
            return new UsurpedMaster();
        }
        if (role instanceof VampireMinion) {
            return new UsurpedVampire();
        }
        if (role instanceof ComteRole) {
            return new UsurpedComte();
        }
        if (role instanceof BansheeRole) {
            return new UsurpedBanshee();
        }
        if (role instanceof GravediggerRole) {
            return new UsurpedGravedigger();
        }
        if (role instanceof ExorcistRole) {
            return new UsurpedExorcist();
        }
        if (role instanceof GremlinRole) {
            return new UsurpedGremlin();
        }
        if (role instanceof PaladinRole) {
            return new UsurpedPaladin();
        }
        if (role instanceof ApprenticeSlayer) {
            return new UsurpedSlayer();
        }
        if (role instanceof SaviorRole) {
            return new UsurpedSavior();
        }
        if (role instanceof ArcherRole) {
            return new UsurpedArcher();
        }
        if (role instanceof BourreauRole) {
            return new UsurpedBourreau();
        }
        if (role instanceof CartographerRole) {
            return new UsurpedCartographer();
        }
        if (role instanceof WatchmanRole) {
            return new UsurpedWatchman();
        }
        if (role instanceof CupidonRole) {
            return new UsurpedCupidon();
        }
        if (role instanceof WeaverRole) {
            return new UsurpedWeaver();
        }
        if (role instanceof SandMerchantRole) {
            return new UsurpedSandMerchant();
        }
        if (role instanceof BabaYagaRole) {
            return new UsurpedBabaYaga();
        }
        if (role instanceof WhiteLadyRole) {
            return new UsurpedWhiteLady();
        }
        return null;
    }

    // --- Cycle de vie ---

    @Override
    public void onEpisodeStart(int episode) {
        if (activePower != null) {
            activePower.onEpisodeStart(episode);
        }
    }

    @Override
    public void onGameEnd() {
        if (activePower != null) {
            activePower.onGameEnd();
        }
        activePower = null;
        usurped = null;
        killer = null;
        killerDamageBonus = false;
        killerKilled = false;
    }

    /**
     * Hook de mort généralisé à tous les rôles.
     *  - mort de l'usurpé → perte du pouvoir + marqueurs, identité du tueur,
     *    +50% de dégâts contre lui (hors auto-kill / mort sans tueur) ;
     *  - mort du tueur → si le Sosie l'a tué : force + speed permanents.
     */
    @Override
    public void onPlayerDeath(VampireUHCPlayer victim, VampireUHCPlayer killerVp) {
        if (usurped != null && usurped.getUuid().equals(victim.getUuid())) {
            onUsurpedDeath(killerVp);
        }
        if (killer != null
                && killer.equals(victim.getUuid())
                && killerVp != null
                && killerVp.getUuid().equals(player.getUuid())) {
            this.killerKilled = true;
            send(ChatColor.GRAY + "Vous êtes parvenu à tuer le tueur de votre cible : "
                    + ChatColor.GREEN + "force + speed permanents !");
        }
    }

    private void onUsurpedDeath(VampireUHCPlayer killerVp) {
        if (activePower != null) {
            activePower.onExit();
        }
        activePower = null;
        usurped = null;

        // Les marqueurs posés PAR LE SOSIE (variantes DOPPELGANGER) s'estompent.
        VampireUHC.getInstance().getMarkerManager().removeMarkersBySource(player.getUuid());

        if (killerVp != null && !killerVp.getUuid().equals(player.getUuid())) {
            this.killer = killerVp.getUuid();
            this.killerDamageBonus = true;
            send(ChatColor.GRAY + "Le joueur dont vous avez copié les pouvoirs est mort, tué par "
                    + ChatColor.GOLD + killerVp.getLastKnownName() + ChatColor.GRAY + "."
                    + "\n" + ChatColor.GRAY + "Vous perdez ses pouvoirs, mais vos dégâts contre "
                    + ChatColor.GOLD + killerVp.getLastKnownName() + ChatColor.GRAY
                    + " augmentent de " + ChatColor.RED + "50% " + ChatColor.GRAY + "de façon permanente.");
        } else {
            send(ChatColor.GRAY + "Le joueur dont vous avez copié les pouvoirs est mort"
                    + " (pas d'identité de tueur confirmée). Ses pouvoirs sont perdus.");
        }
    }

    // --- Accès pour les autres composants ---

    public boolean hasUsed() {
        return used;
    }

    public VampireUHCPlayer getUsurped() {
        return usurped;
    }

    public UsurpedPower getActivePower() {
        return activePower;
    }

    public UUID getKiller() {
        return killer;
    }

    /** +50% de dégâts permanent contre le tueur de l'usurpé (PvPListener). */
    public boolean getDamageBonusAgainst(UUID victimId) {
        return killer != null && killerDamageBonus && killer.equals(victimId);
    }

    /** Tueur pas encore tué : le Sosie garde potentiellement son bonus de mort. */
    public boolean hasKilledKiller() {
        return killerKilled;
    }

    // --- Sérialisation (RoleManager) ---

    public void saveState(JsonObject obj) {
        obj.addProperty("doppelgangerUsed", used);
        if (usurped != null) {
            obj.addProperty("doppelgangerUsurped", usurped.getUuid().toString());
        }
        if (killer != null) {
            obj.addProperty("doppelgangerKiller", killer.toString());
        }
        obj.addProperty("doppelgangerKillerDamageBonus", killerDamageBonus);
        obj.addProperty("doppelgangerKillerKilled", killerKilled);
        if (activePower != null) {
            obj.addProperty("doppelgangerPowerType", activePower.getName());
            activePower.saveState(obj);
        }
    }

    public void restoreState(JsonObject obj, PlayerManager playerManager) {
        used = obj.has("doppelgangerUsed") && obj.get("doppelgangerUsed").getAsBoolean();
        if (!used) {
            return;
        }
        // Cible usurpée restaurée.
        VampireUHCPlayer restoredTarget = obj.has("doppelgangerUsurped")
                && !obj.get("doppelgangerUsurped").isJsonNull()
                ? playerManager.get(UUID.fromString(obj.get("doppelgangerUsurped").getAsString()))
                : null;
        if (restoredTarget != null && restoredTarget.isAlive()) {
            this.usurped = restoredTarget;
            this.activePower = buildPower(restoredTarget);
            if (activePower != null) {
                activePower.onEnter(player);
                activePower.restoreState(obj);
            }
        } else if (restoredTarget != null) {
            // La cible est morte en plein redémarrage : pouvoirs perdus, sans tueur
            // identifié (l'événement de mort n'a pas pu être traité).
            used = true;
        }
        if (obj.has("doppelgangerKiller") && !obj.get("doppelgangerKiller").isJsonNull()) {
            this.killer = UUID.fromString(obj.get("doppelgangerKiller").getAsString());
        }
        this.killerDamageBonus = obj.has("doppelgangerKillerDamageBonus")
                && obj.get("doppelgangerKillerDamageBonus").getAsBoolean();
        this.killerKilled = obj.has("doppelgangerKillerKilled")
                && obj.get("doppelgangerKillerKilled").getAsBoolean();
    }

    private void send(String message) {
        Player bukkit = VampireUHC.getInstance().getServer().getPlayer(player.getUuid());
        if (bukkit != null && bukkit.isOnline()) {
            bukkit.sendMessage(MessageUtil.warn(message));
        }
    }
}