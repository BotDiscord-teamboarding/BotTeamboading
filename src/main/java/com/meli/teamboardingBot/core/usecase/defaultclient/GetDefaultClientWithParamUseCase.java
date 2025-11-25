package com.meli.teamboardingBot.core.usecase.defaultclient;

import com.meli.teamboardingBot.core.ports.defaultclient.GetDefaultClientPort;
import com.meli.teamboardingBot.core.ports.defaultclient.GetDefaultClientWithParamPort;

public class GetDefaultClientWithParamUseCase implements GetDefaultClientWithParamPort {

    private GetDefaultClientPort getDefaultClientPort;

    public GetDefaultClientWithParamUseCase(GetDefaultClientPort getDefaultClientPort) {
        this.getDefaultClientPort = getDefaultClientPort;
    }

    public String get(String endpoint, String queryParams) {
        String fullEndpoint = endpoint + "?" + queryParams;
        return getDefaultClientPort.get(fullEndpoint);
    }

}
