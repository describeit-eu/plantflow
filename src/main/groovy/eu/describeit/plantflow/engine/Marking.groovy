package eu.describeit.plantflow.engine


import groovy.transform.CompileStatic

@CompileStatic
class Marking {
    final List<Place> places
    private final Map<String, Integer> placeIndexMap
    private final List<Token>[] tokenVector

    @SuppressWarnings('unchecked')
    Marking(List<Place> places) {
        this.places = Collections.unmodifiableList(new ArrayList<>(places))
        this.placeIndexMap = new HashMap<>(places.size())
        this.tokenVector = new List[places.size()]
        for (int i = 0; i < places.size(); i++) {
            Place p = places[i]
            placeIndexMap.put(p.id, p.index)
            tokenVector[i] = []
        }
    }

    void addToken(int placeIndex, Token token) {
        if (token == null) return
        tokenVector[placeIndex].add(token)
    }

    void addToken(Place place, Token token) {
        addToken(place.index, token)
    }

    boolean removeToken(int placeIndex, Token token) {
        return tokenVector[placeIndex].remove(token)
    }

    boolean removeToken(Place place, Token token) {
        return removeToken(place.index, token)
    }

    List<Token> getTokens(int placeIndex) {
        return Collections.unmodifiableList(new ArrayList<>(tokenVector[placeIndex]))
    }

    List<Token> getTokens(Place place) {
        return getTokens(place.index)
    }

    List<Token> getTokens(String placeId) {
        Integer idx = placeIndexMap.get(placeId)
        return idx != null ? getTokens(idx) : Collections.emptyList() as List<Token>
    }

    int getTokenCount(int placeIndex) {
        return tokenVector[placeIndex].size()
    }

    int getTokenCount(Place place) {
        return getTokenCount(place.index)
    }

    int getTokenCount(String placeId) {
        Integer idx = placeIndexMap.get(placeId)
        return idx != null ? getTokenCount(idx) : 0
    }

    boolean isEmpty(int placeIndex) {
        return tokenVector[placeIndex].isEmpty()
    }

    boolean isEmpty(Place place) {
        return isEmpty(place.index)
    }

    boolean isEmpty(String placeId) {
        Integer idx = placeIndexMap.get(placeId)
        return idx != null ? isEmpty(idx) : true
    }

    int[] getMarkingVector() {
        int[] vec = new int[tokenVector.length]
        for (int i = 0; i < tokenVector.length; i++) {
            vec[i] = tokenVector[i].size()
        }
        return vec
    }

    Marking copy() {
        Marking copy = new Marking(this.places)
        for (int i = 0; i < tokenVector.length; i++) {
            copy.tokenVector[i].addAll(this.tokenVector[i])
        }
        return copy
    }
}
