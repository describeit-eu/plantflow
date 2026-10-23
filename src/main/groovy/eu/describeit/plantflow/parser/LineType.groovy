package eu.describeit.plantflow.parser

import groovy.transform.CompileStatic

@CompileStatic
enum LineType {
    START, END, ACTION, IF, ELSE, ENDIF, FORK, FORK_AGAIN, END_MERGE, IGNORED
}
