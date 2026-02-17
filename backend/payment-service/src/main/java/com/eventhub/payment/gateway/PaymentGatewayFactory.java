package com.eventhub.payment.gateway;

import com.eventhub.payment.entity.PaymentGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory that resolves the correct PaymentGatewayProvider
 * based on the gateway enum. Uses Spring-injected list of all providers.
 */
@Component
public class PaymentGatewayFactory {

    private final Map<String, PaymentGatewayProvider> providers;

    public PaymentGatewayFactory(List<PaymentGatewayProvider> providerList) {
        this.providers = providerList.stream()
                .collect(Collectors.toMap(
                        PaymentGatewayProvider::getGatewayName,
                        Function.identity()
                ));
    }

    public PaymentGatewayProvider getProvider(PaymentGateway gateway) {
        PaymentGatewayProvider provider = providers.get(gateway.name());
        if (provider == null) {
            throw new IllegalArgumentException("Unsupported payment gateway: " + gateway);
        }
        return provider;
    }

    public PaymentGatewayProvider getProvider(String gatewayName) {
        return getProvider(PaymentGateway.valueOf(gatewayName.toUpperCase()));
    }
}
