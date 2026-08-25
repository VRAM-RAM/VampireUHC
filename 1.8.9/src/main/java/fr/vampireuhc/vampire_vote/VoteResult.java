package fr.vampireuhc.vampire_vote;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public interface VoteResult {

    UUID playerId();

    List<UUID> tiedPlayers();

    final class Winner implements VoteResult {
        private final UUID playerId;

        public Winner(UUID playerId) {
            this.playerId = playerId;
        }

        @Override
        public UUID playerId() {
            return playerId;
        }

        @Override
        public List<UUID> tiedPlayers() {
            return Collections.emptyList();
        }
    }

    final class Tie implements VoteResult {
        private final List<UUID> tiedPlayers;

        public Tie(List<UUID> tiedPlayers) {
            this.tiedPlayers = tiedPlayers;
        }

        @Override
        public UUID playerId() {
            return null;
        }

        @Override
        public List<UUID> tiedPlayers() {
            return tiedPlayers;
        }
    }
}
