package com.prafka.desktop.service;

import com.prafka.desktop.model.ClusterModel;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Singleton
public class SessionService {

    private final CryptoService cryptoService;
    private char[] masterPassword;
    private final AtomicReference<Optional<ClusterModel>> cluster = new AtomicReference<>(Optional.empty());

    @Inject
    public SessionService(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    public char[] getMasterPassword() {
        if (masterPassword == null) throw new IllegalStateException();
        return masterPassword;
    }

    public void setMasterPassword(String masterPassword) {
        this.masterPassword = cryptoService.hashPasswordForSession(masterPassword).toCharArray();
    }

    public Optional<ClusterModel> getClusterOpt() {
        return cluster.get();
    }

    public ClusterModel getCluster() {
        return cluster.get().orElseThrow();
    }

    public void setCluster(ClusterModel cluster) {
        this.cluster.set(Optional.of(cluster));
    }
}
