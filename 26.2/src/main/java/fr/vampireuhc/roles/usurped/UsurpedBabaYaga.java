package fr.vampireuhc.roles.usurped;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.google.gson.JsonObject;

import fr.vampireuhc.config.MessageUtil;
import fr.vampireuhc.player.VampireUHCPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Baba Yaga copiée par le Sosie : uniquement la malédiction (usage unique,
 * 3 minutes). Le Sosie n'hérite PAS de la résurrection — à la mort d'un
 * joueur, il reçoit simplement un message d'information non cliquable.
 */
public class UsurpedBabaYaga implements UsurpedPower {

    // Durée de la malédiction : 3 minutes.
    private static final long CURSE_DURATION_MS = 180_000;

    private VampireUHCPlayer sosie;

    private boolean curseUsed = false;
    private UUID cursedPlayer;
    private long curseExpiry;

    @Override
    public String getName() {
        return "Baba Yaga";
    }

    @Override
    public void onEnter(VampireUHCPlayer doppelganger) {
        this.sosie = doppelganger;
    }

    @Override
    public void onExit() {
    }

    public boolean cursePlayer(VampireUHCPlayer target) {
        if (sosie == null || curseUsed || target == null) {
            return false;
        }
        curseUsed = true;
        cursedPlayer = target.getUuid();
        curseExpiry = System.currentTimeMillis() + CURSE_DURATION_MS;

        Player baba = Bukkit.getPlayer(sosie.getUuid());
        if (baba != null) {
            baba.sendMessage(MessageUtil.successTarget("Vous avez maudit", target.getLastKnownName())
                    .append(Component.text(" pendant 3 minutes !", NamedTextColor.GREEN)));
        }
        Player bukkitTarget = Bukkit.getPlayer(target.getUuid());
        if (bukkitTarget != null) {
            bukkitTarget.sendMessage(MessageUtil.error("Vous avez été maudit par la Baba Yaga : vos pommes d'or ne vous donneront plus d'absorption pendant 3 minutes !"));
        }
        return true;
    }

    // Vrai si le joueur est actuellement sous l'effet de la malédiction.
    public boolean isCurseActive(UUID uuid) {
        return curseUsed && cursedPlayer != null
                && cursedPlayer.equals(uuid)
                && System.currentTimeMillis() <= curseExpiry;
    }

    // Le Sosie n'a pas la résurrection : message informatif, non cliquable.
    public void notifyDeath(VampireUHCPlayer victim) {
        if (sosie == null || victim == null) {
            return;
        }
        Player baba = Bukkit.getPlayer(sosie.getUuid());
        if (baba != null) {
            baba.sendMessage(MessageUtil.serialize(
                    "<dark_purple>Un joueur vient de mourir : <gold>" + victim.getLastKnownName() + "</gold>.</dark_purple>"));
        }
    }

    @Override
    public void saveState(JsonObject obj) {
        obj.addProperty("usurpedBabaCurseUsed", curseUsed);
        obj.addProperty("usurpedBabaCurseExpiry", curseExpiry);
        if (cursedPlayer != null) {
            obj.addProperty("usurpedBabaCursedPlayer", cursedPlayer.toString());
        }
    }

    @Override
    public void restoreState(JsonObject obj) {
        curseUsed = obj.has("usurpedBabaCurseUsed") ? obj.get("usurpedBabaCurseUsed").getAsBoolean() : false;
        curseExpiry = obj.has("usurpedBabaCurseExpiry") ? obj.get("usurpedBabaCurseExpiry").getAsLong() : 0;
        // Une malédiction expirée pendant le redémarrage ne revit pas.
        if (curseUsed && System.currentTimeMillis() <= curseExpiry && obj.has("usurpedBabaCursedPlayer")) {
            cursedPlayer = UUID.fromString(obj.get("usurpedBabaCursedPlayer").getAsString());
        } else {
            curseUsed = false;
            cursedPlayer = null;
            curseExpiry = 0;
        }
    }
}