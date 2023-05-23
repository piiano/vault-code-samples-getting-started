package tokens;

import java.util.List;
import java.util.UUID;

public record TokenDefinition(
        List<String> tokenIds,
        List<UUID> objectIds,
        List<String> tags
) {
    static TokenDefinition fromTokenIds(List<String> tokenIds) {
        return new TokenDefinition(tokenIds, null, null);
    }

    static TokenDefinition fromTags(List<String> tags) {
        return new TokenDefinition(null, null, tags);
    }
}
