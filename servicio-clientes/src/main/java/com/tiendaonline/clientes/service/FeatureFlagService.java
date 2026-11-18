package com.tiendaonline.clientes.service;

import org.springframework.stereotype.Service;

import com.statsig.sdk.Statsig;
import com.statsig.sdk.StatsigUser;

@Service 
public class FeatureFlagService {

    public boolean isMaskPiiEnabled(){
        StatsigUser user = new StatsigUser("servicio-clientes");
        return Statsig.checkGateSync(user, "mask-pii");
    } 
}
