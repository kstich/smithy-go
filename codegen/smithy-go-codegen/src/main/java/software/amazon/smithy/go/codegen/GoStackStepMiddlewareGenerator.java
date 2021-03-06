/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.go.codegen;

import java.util.function.BiConsumer;
import software.amazon.smithy.codegen.core.Symbol;

/**
 * Helper for generating stack step middleware.
 */
public final class GoStackStepMiddlewareGenerator {
    private static final Symbol CONTEXT_TYPE = SymbolUtils.createValueSymbolBuilder(
            "Context", GoDependency.CONTEXT).build();
    private static final Symbol METADATA_TYPE = SymbolUtils.createValueSymbolBuilder(
            "Metadata", GoDependency.SMITHY_MIDDLEWARE).build();

    private final Symbol middlewareSymbol;
    private final String handleMethodName;
    private final Symbol inputType;
    private final Symbol outputType;
    private final Symbol handlerType;

    /**
     * Creates a new middleware generator with the given builder definition.
     *
     * @param builder the builder to create the generator with.
     */
    public GoStackStepMiddlewareGenerator(Builder builder) {
        this.middlewareSymbol = SymbolUtils.createPointableSymbolBuilder(builder.identifier).build();
        this.handleMethodName = builder.handleMethodName;
        this.inputType = builder.inputType;
        this.outputType = builder.outputType;
        this.handlerType = builder.handlerType;
    }

    /**
     * Create a new SerializeStep middleware generator with the provided type name.
     *
     * @param identifier is the type name to identify the middleware.
     * @return the middleware generator.
     */
    public static GoStackStepMiddlewareGenerator createSerializeStepMiddleware(String identifier) {
        return createMiddleware(identifier,
                "HandleSerialize",
                SymbolUtils.createValueSymbolBuilder("SerializeInput", GoDependency.SMITHY_MIDDLEWARE).build(),
                SymbolUtils.createValueSymbolBuilder("SerializeOutput", GoDependency.SMITHY_MIDDLEWARE).build(),
                SymbolUtils.createValueSymbolBuilder("SerializeHandler", GoDependency.SMITHY_MIDDLEWARE).build());
    }

    /**
     * Create a new DeserializeStep middleware generator with the provided type name.
     *
     * @param identifier is the type name to identify the middleware.
     * @return the middleware generator.
     */
    public static GoStackStepMiddlewareGenerator createDeserializeStepMiddleware(String identifier) {
        return createMiddleware(identifier,
                "HandleDeserialize",
                SymbolUtils.createValueSymbolBuilder("DeserializeInput", GoDependency.SMITHY_MIDDLEWARE).build(),
                SymbolUtils.createValueSymbolBuilder("DeserializeOutput", GoDependency.SMITHY_MIDDLEWARE).build(),
                SymbolUtils.createValueSymbolBuilder("DeserializeHandler", GoDependency.SMITHY_MIDDLEWARE).build());
    }

    /**
     * Generates a new step middleware generator.
     *
     * @param identifier        the name of the middleware type.
     * @param handlerMethodName identifier method name to be implemented.
     * @param inputType         the middleware input type.
     * @param outputType        the middleware output type.
     * @param handlerType       the next handler type.
     * @return the middleware generator.
     */
    public static GoStackStepMiddlewareGenerator createMiddleware(
            String identifier,
            String handlerMethodName,
            Symbol inputType,
            Symbol outputType,
            Symbol handlerType
    ) {
        return builder()
                .identifier(identifier)
                .handleMethodName(handlerMethodName)
                .inputType(inputType)
                .outputType(outputType)
                .handlerType(handlerType)
                .build();
    }


    /**
     * Writes the middleware definition to the provided writer.
     * See the writeMiddleware overloaded function signature for a more complete definition.
     *
     * @param writer              the writer to which the middleware definition will be written to.
     * @param handlerBodyConsumer is a consumer that will be call in the context of populating the handler definition.
     */
    public void writeMiddleware(
            GoWriter writer,
            BiConsumer<GoStackStepMiddlewareGenerator, GoWriter> handlerBodyConsumer
    ) {
        writeMiddleware(writer, handlerBodyConsumer, (m, w) -> {
        });
    }

    /**
     * Writes the middleware definition to the provided writer.
     * <p>
     * The following Go variables will be in scope of the handler body:
     * ctx - the Go standard library context.Context type.
     * in - the input for the given middleware type.
     * next - the next handler to be called.
     * out - the output for the given middleware type.
     * metadata - the smithy middleware.Metadata type.
     * err - the error interface type.
     *
     * @param writer              the writer to which the middleware definition will be written to.
     * @param handlerBodyConsumer is a consumer that will be called in the context of populating the handler definition.
     * @param fieldConsumer       is a consumer that will be called in the context of populating the struct members.
     */
    public void writeMiddleware(
            GoWriter writer,
            BiConsumer<GoStackStepMiddlewareGenerator, GoWriter> handlerBodyConsumer,
            BiConsumer<GoStackStepMiddlewareGenerator, GoWriter> fieldConsumer
    ) {
        writer.addUseImports(CONTEXT_TYPE);
        writer.addUseImports(METADATA_TYPE);
        writer.addUseImports(inputType);
        writer.addUseImports(outputType);
        writer.addUseImports(handlerType);

        // generate the structure type definition for the middleware
        writer.openBlock("type $L struct {", "}", middlewareSymbol, () -> {
            fieldConsumer.accept(this, writer);
        });

        writer.write("");

        // each middleware step has to implement the ID function and return a unique string to identify itself with
        // here we return the name of the type
        writer.openBlock("func ($P) ID() string {", "}", middlewareSymbol, () -> {
            writer.openBlock("return $S", middlewareSymbol);
        });

        writer.write("");

        // each middleware must implement their given handlerMethodName in order to satisfy the interface for
        // their respective step.
        writer.openBlock("func (m $P) $L(ctx $T, in $T, next $T) (\n"
                       + "\tout $T, metadata $T, err error,\n"
                       + ") {", "}",
                new Object[]{
                        middlewareSymbol, handleMethodName, CONTEXT_TYPE, inputType, handlerType, outputType,
                        METADATA_TYPE,
                },
                () -> {
                    handlerBodyConsumer.accept(this, writer);
                });
    }

    /**
     * Returns a new middleware generator builder.
     *
     * @return the middleware generator builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Get the handle method name.
     *
     * @return handler method name.
     */
    public String getHandleMethodName() {
        return handleMethodName;
    }

    /**
     * Get the middleware type symbol.
     *
     * @return Symbol for the middleware type.
     */
    public Symbol getMiddlewareSymbol() {
        return middlewareSymbol;
    }

    /**
     * Get the input type symbol reference.
     *
     * @return the input type symbol reference.
     */
    public Symbol getInputType() {
        return inputType;
    }

    /**
     * Get the output type symbol reference.
     *
     * @return the output type symbol reference.
     */
    public Symbol getOutputType() {
        return outputType;
    }

    /**
     * Get the handler type symbol reference.
     *
     * @return the handler type symbol reference.
     */
    public Symbol getHandlerType() {
        return handlerType;
    }

    /**
     * Get the context type symbol.
     *
     * @return the context type symbol.
     */
    public static Symbol getContextType() {
        return CONTEXT_TYPE;
    }

    /**
     * Get the middleware metadata type symbol.
     *
     * @return the middleware metadata type symbol.
     */
    public static Symbol getMiddlewareMetadataType() {
        return METADATA_TYPE;
    }

    /**
     * Builds a {@link GoStackStepMiddlewareGenerator}.
     */
    public static class Builder {
        private String identifier;
        private String handleMethodName;
        private Symbol inputType;
        private Symbol outputType;
        private Symbol handlerType;

        /**
         * Builds the middleware generator.
         *
         * @return the middleware generator.
         */
        public GoStackStepMiddlewareGenerator build() {
            return new GoStackStepMiddlewareGenerator(this);
        }

        /**
         * Sets the handler method name.
         *
         * @param handleMethodName the middleware handler method name to implement.
         * @return the builder.
         */
        public Builder handleMethodName(String handleMethodName) {
            this.handleMethodName = handleMethodName;
            return this;
        }

        /**
         * Set the identifier of the type to be generated.
         *
         * @param identifier the name to identify the middleware type.
         * @return the builder.
         */
        public Builder identifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        /**
         * Set the input type symbol reference for the middleware.
         *
         * @param inputType the symbol reference to the input type.
         * @return the builder.
         */
        public Builder inputType(Symbol inputType) {
            this.inputType = inputType;
            return this;
        }

        /**
         * Set the output type symbol reference for the middleware.
         *
         * @param outputType the symbol reference to the output type.
         * @return the builder.
         */
        public Builder outputType(Symbol outputType) {
            this.outputType = outputType;
            return this;
        }

        /**
         * Set the hander type symbol reference for the middleware.
         *
         * @param handlerType the symbol reference to the handler type.
         * @return the builder.
         */
        public Builder handlerType(Symbol handlerType) {
            this.handlerType = handlerType;
            return this;
        }
    }
}
