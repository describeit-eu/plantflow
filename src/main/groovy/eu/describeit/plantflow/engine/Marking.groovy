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

    Marking(int numberOfPlaces) {
        this.tokens = new Token[numberOfPlaces][0]
    }

    @JsonCreator
    Marking(@JsonProperty('tokens') Token[][] tokens) {
        this.tokens = tokens
    }

    void addToken(int placeIndex, Token token) {
        if (token == null) return
        if (placeIndex >= 0 && placeIndex < tokens.length) {
            tokens[placeIndex] = tokens[placeIndex] + token
        }
    }

    void addToken(Place place, Token token) {
        addToken(place.index, token)
    }

    boolean removeToken(int placeIndex, Token token) {
        if (placeIndex < 0 || placeIndex >= tokens.length) {
            return false
        }
        int origSize = tokens[placeIndex].length
        tokens[placeIndex] = tokens[placeIndex] - (token)
        return tokens[placeIndex].length < origSize
    }

    boolean removeToken(Place place, Token token) {
        return removeToken(place.index, token)
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

    @JsonProperty('tokens')
    Token[][] getTokens() {
        return tokens
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
        return copy
    }
}
