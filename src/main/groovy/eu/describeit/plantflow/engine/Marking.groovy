package eu.describeit.plantflow.engine

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.CompileStatic

@CompileStatic
@JsonIgnoreProperties(['metaClass'])
class Marking {

    final Token[][] tokens

    private final Map<Integer, List<Integer>> pendingForkActivations
    private final Map<Integer, List<Integer>> selectedForkActivations
    private final Map<Integer, List<Integer>> tokenActivations
    private int nextForkActivationId

    Marking(int numberOfPlaces) {
        this.tokens = new Token[numberOfPlaces][0]
        this.pendingForkActivations = [:]
        this.selectedForkActivations = [:]
        this.tokenActivations = [:]
        this.nextForkActivationId = 1
    }

    @JsonCreator
    Marking(@JsonProperty('tokens') Token[][] tokens,
            @JsonProperty('pendingForkActivationIds') Map<Integer, List<Integer>> pendingForkActivationIds,
            @JsonProperty('selectedForkActivationIds') Map<Integer, List<Integer>> selectedForkActivationIds,
            @JsonProperty('tokenActivations') Map<Integer, List<Integer>> tokenActivations,
            @JsonProperty('nextForkActivationId') Integer nextForkActivationId) {
        this.tokens = tokens
        this.pendingForkActivations = [:]
        this.selectedForkActivations = [:]
        this.tokenActivations = [:]
        this.nextForkActivationId = nextForkActivationId != null ? nextForkActivationId : 1
        copyActivationIds(pendingForkActivationIds, this.pendingForkActivations)
        copyActivationIds(selectedForkActivationIds, this.selectedForkActivations)
        for (int i = 0; i < this.tokens.length; i++) {
            tokenActivationList(i)
        }
        if (tokenActivations != null) {
            tokenActivations.each { Integer placeIndex, List<Integer> ids ->
                if (placeIndex != null && placeIndex >= 0 && placeIndex < this.tokens.length) {
                    List<Integer> tags = tokenActivationList(placeIndex)
                    for (int i = 0; i < ids.size() && i < tags.size(); i++) {
                        tags.set(i, ids.get(i))
                    }
                }
            }
        }
    }

    void addToken(int placeIndex, Token token, Integer activationId = null) {
        if (token == null) return
        if (placeIndex >= 0 && placeIndex < tokens.length) {
            List<Integer> tags = tokenActivationList(placeIndex)
            tokens[placeIndex] = tokens[placeIndex] + token
            tags.add(activationId)
        }
    }

    void addToken(Place place, Token token, Integer activationId = null) {
        addToken(place.index, token, activationId)
    }

    boolean removeToken(int placeIndex, Token token) {
        if (placeIndex < 0 || placeIndex >= tokens.length) {
            return false
        }
        Token[] placeTokens = tokens[placeIndex]
        for (int i = 0; i < placeTokens.length; i++) {
            if (placeTokens[i] != null && placeTokens[i] == token) {
                removeTokenAt(placeIndex, i)
                return true
            }
        }
        return false
    }

    Token removeTokenAt(int placeIndex, int position) {
        if (placeIndex < 0 || placeIndex >= tokens.length) {
            return null
        }
        if (position < 0 || position >= tokens[placeIndex].length) {
            return null
        }
        List<Token> placeTokens = new ArrayList<>(Arrays.asList(tokens[placeIndex]))
        Token removed = placeTokens.remove(position)
        tokens[placeIndex] = placeTokens.toArray(new Token[0])
        List<Integer> tags = tokenActivations.get(placeIndex)
        if (tags != null && position < tags.size()) {
            tags.remove(position)
        }
        return removed
    }

    List<Token> getTokens(int placeIndex) {
        if (placeIndex < 0 || placeIndex >= tokens.length) {
            return Collections.emptyList() as List<Token>
        }
        return Collections.unmodifiableList(Arrays.asList(tokens[placeIndex]))
    }

    List<Token> getTokens(Place place) {
        return getTokens(place.index)
    }

    int getTokenCount(int placeIndex) {
        if (placeIndex < 0 || placeIndex >= tokens.length) {
            return 0
        }
        return tokens[placeIndex].length
    }

    int getTokenCount(Place place) {
        return getTokenCount(place.index)
    }

    boolean isEmpty(int placeIndex) {
        if (placeIndex < 0 || placeIndex >= tokens.length) {
            return true
        }
        return tokens[placeIndex].length == 0
    }

    boolean isEmpty(Place place) {
        return isEmpty(place.index)
    }

    Integer getTokenActivation(int placeIndex, int position) {
        List<Integer> tags = tokenActivations.get(placeIndex)
        if (tags == null || position < 0 || position >= tags.size()) {
            return null
        }
        return tags.get(position)
    }

    @JsonProperty('tokens')
    Token[][] getTokens() {
        return tokens
    }

    int registerForkActivation(int forkTransitionIndex) {
        int activationId = nextForkActivationId
        nextForkActivationId = nextForkActivationId + 1
        activationIdList(pendingForkActivations, forkTransitionIndex).add(activationId)
        return activationId
    }

    int getPendingForkActivations(int forkTransitionIndex) {
        List<Integer> pending = pendingForkActivations.get(forkTransitionIndex)
        return pending == null ? 0 : pending.size()
    }

    boolean consumeForkActivation(int forkTransitionIndex) {
        List<Integer> pending = pendingForkActivations.get(forkTransitionIndex)
        if (pending == null || pending.isEmpty()) {
            return false
        }
        int activationId = pending.remove(0)
        activationIdList(selectedForkActivations, forkTransitionIndex).add(activationId)
        return true
    }

    boolean isForkActivationPending(int forkTransitionIndex, int activationId) {
        List<Integer> pending = pendingForkActivations.get(forkTransitionIndex)
        return pending != null && pending.contains(activationId)
    }

    boolean selectForkActivation(int forkTransitionIndex, int activationId) {
        List<Integer> pending = pendingForkActivations.get(forkTransitionIndex)
        if (pending == null || !pending.remove((Object) Integer.valueOf(activationId))) {
            return false
        }
        activationIdList(selectedForkActivations, forkTransitionIndex).add(activationId)
        return true
    }

    List<Integer> getSelectedForkActivations(int forkTransitionIndex) {
        List<Integer> selected = selectedForkActivations.get(forkTransitionIndex)
        return selected == null ? Collections.emptyList() : Collections.unmodifiableList(selected)
    }

    @JsonProperty('forkActivations')
    Map<Integer, Integer> getForkActivations() {
        Map<Integer, Integer> counts = [:]
        pendingForkActivations.each { Integer forkIndex, List<Integer> pending ->
            counts.put(forkIndex, pending.size())
        }
        return Collections.unmodifiableMap(counts)
    }

    @JsonProperty('forkActivations')
    void setForkActivations(Map<Integer, Integer> counts) {
        if (counts == null) {
            return
        }
        boolean hasPendingIds = pendingForkActivations.values().any { List<Integer> ids -> !ids.isEmpty() }
        if (hasPendingIds) {
            return
        }
        counts.each { Integer forkIndex, Integer count ->
            for (int i = 0; i < count; i++) {
                registerForkActivation(forkIndex)
            }
        }
    }

    @JsonProperty('pendingForkActivationIds')
    Map<Integer, List<Integer>> getPendingForkActivationIds() {
        return activationIdSnapshot(pendingForkActivations)
    }

    @JsonProperty('selectedForkActivationIds')
    Map<Integer, List<Integer>> getSelectedForkActivationIds() {
        return activationIdSnapshot(selectedForkActivations)
    }

    @JsonProperty('tokenActivations')
    Map<Integer, List<Integer>> getTokenActivations() {
        return activationIdSnapshot(tokenActivations)
    }

    @JsonProperty('nextForkActivationId')
    int getNextForkActivationId() {
        return nextForkActivationId
    }

    @JsonIgnore
    int[] getMarkingVector() {
        int[] vec = new int[tokens.length]
        for (int i = 0; i < tokens.length; i++) {
            vec[i] = tokens[i].length
        }
        return vec
    }

    Marking copy() {
        Marking copy = new Marking(this.tokens.length)
        for (int i = 0; i < tokens.length; i++) {
            copy.tokens[i] = Arrays.copyOf(this.tokens[i], this.tokens[i].length)
        }
        copyActivationIds(this.@pendingForkActivations, copy.@pendingForkActivations)
        copyActivationIds(this.@selectedForkActivations, copy.@selectedForkActivations)
        copyActivationIds(this.@tokenActivations, copy.@tokenActivations)
        copy.nextForkActivationId = this.nextForkActivationId
        return copy
    }

    private static void copyActivationIds(Map<Integer, List<Integer>> source, Map<Integer, List<Integer>> target) {
        if (source == null) {
            return
        }
        source.each { Integer key, List<Integer> ids ->
            target.put(key, new ArrayList<>(ids))
        }
    }

    private static Map<Integer, List<Integer>> activationIdSnapshot(Map<Integer, List<Integer>> source) {
        Map<Integer, List<Integer>> snapshot = [:]
        source.each { Integer key, List<Integer> ids ->
            snapshot.put(key, Collections.unmodifiableList(new ArrayList<>(ids)))
        }
        return Collections.unmodifiableMap(snapshot)
    }

    private static List<Integer> activationIdList(Map<Integer, List<Integer>> map, int forkTransitionIndex) {
        List<Integer> ids = map.get(forkTransitionIndex)
        if (ids == null) {
            ids = []
            map.put(forkTransitionIndex, ids)
        }
        return ids
    }

    private List<Integer> tokenActivationList(int placeIndex) {
        List<Integer> tags = tokenActivations.get(placeIndex)
        if (tags == null) {
            tags = []
            tokenActivations.put(placeIndex, tags)
        }
        while (tags.size() < tokens[placeIndex].length) {
            tags.add(null)
        }
        return tags
    }
}
