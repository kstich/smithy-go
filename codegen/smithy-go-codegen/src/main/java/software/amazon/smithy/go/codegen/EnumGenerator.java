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

import java.util.Locale;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.traits.EnumDefinition;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.utils.StringUtils;

/**
 * Renders enums and their constants.
 */
final class EnumGenerator implements Runnable {

    private final SymbolProvider symbolProvider;
    private final GoWriter writer;
    private final StringShape shape;

    EnumGenerator(SymbolProvider symbolProvider, GoWriter writer, StringShape shape) {
        this.symbolProvider = symbolProvider;
        this.writer = writer;
        this.shape = shape;
    }

    @Override
    public void run() {
        Symbol symbol = symbolProvider.toSymbol(shape);
        EnumTrait enumTrait = shape.expectTrait(EnumTrait.class);

        writer.write("type $L string", symbol.getName()).write("");

        writer.write("// Enum values for $L", symbol.getName()).openBlock("const (", ")", () -> {
            for (EnumDefinition definition : enumTrait.getValues()) {
                StringBuilder labelBuilder = new StringBuilder(symbol.getName());
                String name = definition.getName().orElse(definition.getValue());
                for (String part : name.split("(?U)\\W")) {
                    labelBuilder.append(StringUtils.capitalize(part.toLowerCase(Locale.US)));
                }
                String label = labelBuilder.toString();
                definition.getDocumentation().ifPresent(writer::writeDocs);
                writer.write("$L $L = $S", label, symbol.getName(), definition.getValue());
            }
        }).write("");
    }
}
