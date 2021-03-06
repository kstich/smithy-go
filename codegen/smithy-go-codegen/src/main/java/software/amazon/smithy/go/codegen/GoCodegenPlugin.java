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

import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.SmithyBuildPlugin;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.Model;

/**
 * Plugin to trigger Go code generation.
 */
public final class GoCodegenPlugin implements SmithyBuildPlugin {
    @Override
    public String getName() {
        return "go-codegen";
    }

    @Override
    public void execute(PluginContext context) {
        new CodegenVisitor(context).execute();
    }

    /**
     * Creates a Go symbol provider.
     * @param model The model to generate symbols for.
     * @param rootModuleName The name of the package root.
     * @return Returns the created provider.
     */
    public static SymbolProvider createSymbolProvider(Model model, String rootModuleName) {
        return new SymbolVisitor(model, rootModuleName);
    }
}
