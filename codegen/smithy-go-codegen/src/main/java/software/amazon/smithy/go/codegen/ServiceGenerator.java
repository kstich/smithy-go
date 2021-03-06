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

import java.util.List;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.go.codegen.integration.GoIntegration;
import software.amazon.smithy.go.codegen.integration.RuntimeClientPlugin;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.traits.StringTrait;
import software.amazon.smithy.model.traits.TitleTrait;

/**
 * Generates a service client and configuration.
 */
final class ServiceGenerator implements Runnable {

    public static final String CONFIG_NAME = "Options";
    public static final String API_OPTIONS_FUNC_NAME = "APIOptionFunc";

    private final GoSettings settings;
    private final Model model;
    private final SymbolProvider symbolProvider;
    private final GoWriter writer;
    private final ServiceShape service;
    private final List<GoIntegration> integrations;
    private final List<RuntimeClientPlugin> runtimePlugins;
    private final ApplicationProtocol applicationProtocol;

    ServiceGenerator(
            GoSettings settings,
            Model model,
            SymbolProvider symbolProvider,
            GoWriter writer,
            ServiceShape service,
            List<GoIntegration> integrations,
            List<RuntimeClientPlugin> runtimePlugins,
            ApplicationProtocol applicationProtocol
    ) {
        this.settings = settings;
        this.model = model;
        this.symbolProvider = symbolProvider;
        this.writer = writer;
        this.service = service;
        this.integrations = integrations;
        this.runtimePlugins = runtimePlugins;
        this.applicationProtocol = applicationProtocol;
    }

    @Override
    public void run() {
        Symbol serviceSymbol = symbolProvider.toSymbol(service);
        writer.writeShapeDocs(service);
        writer.openBlock("type $T struct {", "}", serviceSymbol, () -> {
            writer.write("options $L", CONFIG_NAME);
        });

        generateConstructor(serviceSymbol);

        String clientId = CodegenUtils.getDefaultPackageImportName(settings.getModuleName());
        String clientTitle = service.getTrait(TitleTrait.class).map(StringTrait::getValue).orElse(clientId);

        writer.writeDocs("ServiceID returns the name of the identifier for the service API.");
        writer.write("func (c $P) ServiceID() string { return $S }", serviceSymbol, clientId);

        writer.writeDocs("ServiceName returns the full service title.");
        writer.write("func (c $P) ServiceName() string { return $S }", serviceSymbol, clientTitle);

        generateConfig();

        Symbol stackSymbol = SymbolUtils.createPointableSymbolBuilder("Stack", GoDependency.SMITHY_MIDDLEWARE).build();
        writer.write("type $L func($P) error", API_OPTIONS_FUNC_NAME, stackSymbol);
    }

    private void generateConstructor(Symbol serviceSymbol) {
        writer.writeDocs(String.format("New returns an initialized %s based on the functional options. "
                    + "Provide additional functional options to further configure the behavior "
                    + "of the client, such as changing the client's endpoint or adding custom "
                    + "middleware behavior.", serviceSymbol.getName()));
        writer.openBlock("func New(options $L) ($P, error) {", "}",
                CONFIG_NAME, serviceSymbol, () -> {

            writer.write("options = options.Copy()").write("");

            // Run any config initialization functions registered by runtime plugins.
            for (RuntimeClientPlugin runtimeClientPlugin : runtimePlugins) {
                if (!runtimeClientPlugin.matchesService(model, service)
                        || !runtimeClientPlugin.getResolveFunction().isPresent()) {
                    continue;
                }
                writer.write("if err := $T(&options); err != nil { return nil, err }",
                        runtimeClientPlugin.getResolveFunction().get());
                writer.write("");
            }

            writer.openBlock("client := &$T {", "}", serviceSymbol, () -> {
                writer.write("options: options,");
            }).write("");

            writer.write("return client, nil");
        });
    }

    private void generateConfig() {
        writer.openBlock("type $L struct {", "}", CONFIG_NAME, () -> {
            writer.writeDocs("Set of options to modify how an operation is invoked. These apply to all operations "
                    + "invoked for this client. Use functional options on operation call to modify this "
                    + "list for per operation behavior."
            );
            writer.write("APIOptions []$L", API_OPTIONS_FUNC_NAME).write("");

            for (GoIntegration integration : integrations) {
                integration.addConfigFields(settings, model, symbolProvider, writer);
            }

            generateApplicationProtocolConfig();
        }).write("");

        generateApplicationProtocolTypes();

        writer.writeDocs("Copy creates a clone where the APIOptions list is deep copied.");
        writer.openBlock("func (o $L) Copy() $L {", "}", CONFIG_NAME, CONFIG_NAME, () -> {
            writer.write("to := o");
            writer.write("to.APIOptions = make([]$L, len(o.APIOptions))", API_OPTIONS_FUNC_NAME);
            writer.write("copy(to.APIOptions, o.APIOptions)");
            writer.write("return to");
        });
    }

    private void generateApplicationProtocolConfig() {
        ensureSupportedProtocol();
        writer.writeDocs(
                "The HTTP client to invoke API calls with. Defaults to client's default HTTP implementation if nil.");
        writer.write("HTTPClient HTTPClient").write("");
    }

    private void generateApplicationProtocolTypes() {
        ensureSupportedProtocol();
        writer.openBlock("type HTTPClient interface {", "}", () -> {
            writer.write("Do($P) ($P, error)", applicationProtocol.getRequestType(),
                    applicationProtocol.getResponseType());
        }).write("");
    }

    private void ensureSupportedProtocol() {
        if (!applicationProtocol.isHttpProtocol()) {
            throw new UnsupportedOperationException(
                    "Protocols other than HTTP are not yet implemented: " + applicationProtocol);
        }
    }
}
