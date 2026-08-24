package fr.vampireuhc.vampire_vote;

import java.util.List;
import java.util.UUID;

public sealed interface VoteResult permits VoteResult.Winner, VoteResult.Tie {
    record Winner(UUID playerId) implements VoteResult {}
    record Tie(List<UUID> tiedPlayers) implements VoteResult {}
}
