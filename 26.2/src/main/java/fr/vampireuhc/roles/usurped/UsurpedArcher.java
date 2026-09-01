package fr.vampireuhc.roles.usurped;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.google.gson.JsonObject;

import fr.vampireuhc.VampireUHC;
import fr.vampireuhc.player.VampireUHCPlayer;

/**
 * Archer copié par le Sosie : passif de marquage lumineux identique au vrai
 * Archer (bibliothèque GlowingEntities), sans l'équipement ni les livres.
 *
 * <p>Le glow ciblé est retiré automatiquement après 15 secondes, comme le
 * vrai Archer — aucun nettoyage n'est nécessaire à la sortie.
 */
public class UsurpedArcher implements UsurpedPower {

    private VampireUHCPlayer sosie;

    @Override
    public String getName() {
        return "Archer";
    }

    @Override
    public void onEnter(VampireUHCPlayer doppelganger) {
        this.sosie = doppelganger;
    }

    @Override
    public void onExit() {
    }

    public void setGlowOnHit(Entity target, VampireUHC plugin) {
        if (sosie == null) {
            return;
        }
        Player bukkitArcher = Bukkit.getPlayer(sosie.getUuid());
        if (bukkitArcher == null) {
            return;
        }
        setGlow(target, bukkitArcher, plugin);
        Bukkit.getScheduler().runTaskLater(plugin,
                () -> removeGlow(target, bukkitArcher, plugin), 20L * 15L);
    }

    private void setGlow(Entity target, Player viewer, VampireUHC plugin) {
        try {
            plugin.getGlowingEntities().setGlowing(target, viewer);
        } catch (ReflectiveOperationException e) {
            plugin.getLogger().log(
                Level.SEVERE,
                "Impossible d'appliquer le glowing (Sosie-Archer)",
                e
            );
        }
    }

    private void removeGlow(Entity target, Player viewer, VampireUHC plugin) {
        try {
            plugin.getGlowingEntities().unsetGlowing(target, viewer);
        } catch (ReflectiveOperationException e) {
            plugin.getLogger().log(
                Level.SEVERE,
                "Impossible de retirer le glowing (Doppelganger-Archer)",
                e
            );
        }
    }

    @Override
    public void saveState(JsonObject obj) {
    }

    @Override
    public void restoreState(JsonObject obj) {
    }
}