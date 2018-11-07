package mozilla.components.lib.jexl.parser

import mozilla.components.lib.jexl.ast.AstNode
import mozilla.components.lib.jexl.ast.AstType
import mozilla.components.lib.jexl.lexer.Token

internal val stateMachine: Map<State, StateMap> = mapOf(
    State.EXPECT_OPERAND to StateMap(
        mapOf(
            Token.Type.LITERAL to NextState(State.EXPECT_BIN_OP),
            Token.Type.IDENTIFIER to NextState(
                State.IDENTIFIER
            ),
            Token.Type.UNARY_OP to NextState(),
            Token.Type.OPEN_PAREN to NextState(
                State.SUB_EXPRESSION
            ),
            Token.Type.OPEN_CURL to NextState(
                State.EXPECT_OBJECT_KEY,
                ::objectStart
            ),
            Token.Type.DOT to NextState(State.TRAVERSE),
            Token.Type.OPEN_BRACKET to NextState(
                State.ARRAY_VALUE,
                ::arrayStart
            )
        )
    ),
    State.EXPECT_BIN_OP to StateMap(
        mapOf(
            Token.Type.BINARY_OP to NextState(
                State.EXPECT_OPERAND
            ),
            Token.Type.PIPE to NextState(State.EXPECT_TRANSFORM),
            Token.Type.DOT to NextState(State.TRAVERSE),
            Token.Type.QUESTION to NextState(
                State.TERNARY_MID,
                ::ternaryStart
            )
        ),
        completable = true
    ),
    State.EXPECT_TRANSFORM to StateMap(
        mapOf(
            Token.Type.IDENTIFIER to NextState(
                State.POST_TRANSFORM,
                ::transform
            )
        )
    ),
    State.EXPECT_OBJECT_KEY to StateMap(
        mapOf(
            Token.Type.IDENTIFIER to NextState(
                State.EXPECT_KEY_VALUE_SEPARATOR,
                ::objectKey
            ),
            Token.Type.CLOSE_CURL to NextState(
                State.EXPECT_BIN_OP
            )
        )
    ),
    State.EXPECT_KEY_VALUE_SEPARATOR to StateMap(
        mapOf(
            Token.Type.COLON to NextState(State.OBJECT_VALUE)
        )
    ),
    State.POST_TRANSFORM to StateMap(
        mapOf(
            Token.Type.OPEN_PAREN to NextState(
                State.ARGUMENT_VALUE
            ),
            Token.Type.BINARY_OP to NextState(
                State.EXPECT_OPERAND
            ),
            Token.Type.DOT to NextState(State.TRAVERSE),
            Token.Type.OPEN_BRACKET to NextState(
                State.FILTER
            ),
            Token.Type.PIPE to NextState(State.EXPECT_TRANSFORM)
        ),
        completable = true
    ),
    State.POST_TRANSFORM_ARGUMENTS to StateMap(
        mapOf(
            Token.Type.BINARY_OP to NextState(
                State.EXPECT_OPERAND
            ),
            Token.Type.DOT to NextState(State.TRAVERSE),
            Token.Type.OPEN_BRACKET to NextState(
                State.FILTER
            ),
            Token.Type.PIPE to NextState(State.EXPECT_TRANSFORM)
        ),
        completable = true
    ),
    State.IDENTIFIER to StateMap(
        mapOf(
            Token.Type.BINARY_OP to NextState(
                State.EXPECT_OPERAND
            ),
            Token.Type.DOT to NextState(State.TRAVERSE),
            Token.Type.OPEN_BRACKET to NextState(
                State.FILTER
            ),
            Token.Type.PIPE to NextState(State.EXPECT_TRANSFORM),
            Token.Type.QUESTION to NextState(
                State.TERNARY_MID,
                ::ternaryStart
            )
        ),
        completable = true
    ),
    State.TRAVERSE to StateMap(
        mapOf(
            Token.Type.IDENTIFIER to NextState(
                State.IDENTIFIER
            )
        )
    ),
    State.FILTER to StateMap(
        subHandler = { parser, node ->
            val expressionNode = AstNode(
                AstType.FILTER_EXPRESSION,
                expression = node,
                relative = parser.subParser!!.relative,
                subject = parser.cursor
            )
            parser.placeBeforeCursor(expressionNode)
        },
        endStates = mapOf(
            Token.Type.CLOSE_BRACKET to State.IDENTIFIER
        )
    ),
    State.SUB_EXPRESSION to StateMap(
        subHandler = { parser, node ->
            parser.placeAtCursor(node!!)
        },
        endStates = mapOf(
            Token.Type.CLOSE_PAREN to State.EXPECT_BIN_OP
        )
    ),
    State.ARGUMENT_VALUE to StateMap(
        subHandler = { parser, node ->
            parser.cursor!!.arguments.add(node!!)
        },
        endStates = mapOf(
            Token.Type.COMMA to State.ARGUMENT_VALUE,
            Token.Type.CLOSE_PAREN to State.EXPECT_BIN_OP
        )
    ),
    State.OBJECT_VALUE to StateMap(
        subHandler = { parser, node ->
            @Suppress("UNCHECKED_CAST")
            (parser.cursor!!.value as MutableMap<String, AstNode>)[parser.currentObjectKey!!] =
                node!!
        },
        endStates = mapOf(
            Token.Type.COMMA to State.EXPECT_OBJECT_KEY,
            Token.Type.CLOSE_CURL to State.EXPECT_BIN_OP
        )
    ),
    State.ARRAY_VALUE to StateMap(
        subHandler = { parser, node ->
            if (node != null) {
                @Suppress("UNCHECKED_CAST")
                (parser.cursor!!.value as MutableList<AstNode>).add(node)
            }
        },
        endStates = mapOf(
            Token.Type.COMMA to State.ARRAY_VALUE,
            Token.Type.CLOSE_BRACKET to State.EXPECT_BIN_OP
        )
    ),
    State.TERNARY_MID to StateMap(
        subHandler = { parser, node ->
            parser.cursor!!.consequent = node
        },
        endStates = mapOf(
            Token.Type.COLON to State.TERNARY_END
        )
    ),
    State.TERNARY_END to StateMap(
        subHandler = { parser, node ->
            parser.cursor!!.alternate = node
        },
        completable = true
    )
)

private fun objectStart(parser: Parser, @Suppress("UNUSED_PARAMETER") token: Token) {
    val node = AstNode(
        AstType.OBJECT_LITERAL,
        mutableMapOf<String, AstNode>()
    )
    parser.placeAtCursor(node)
}

private fun objectKey(parser: Parser, token: Token) {
    parser.currentObjectKey = token.value.toString()
}

private fun arrayStart(parser: Parser, @Suppress("UNUSED_PARAMETER") token: Token) {
    val node = AstNode(
        AstType.ARRAY_LITERAL,
        mutableListOf<AstNode>()
    )
    parser.placeAtCursor(node)
}

private fun transform(parser: Parser, token: Token) {
    val node = AstNode(
        AstType.TRANSFORM,
        name = token.value.toString(),
        subject = parser.cursor
    )
    parser.placeBeforeCursor(node)
}

private fun ternaryStart(parser: Parser, @Suppress("UNUSED_PARAMETER") token: Token) {
    val node = AstNode(
        AstType.CONDITIONAL_EXPRESSION,
        test = parser.tree
    )
    parser.tree = node
    parser.cursor = node
}
