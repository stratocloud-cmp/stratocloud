package com.stratocloud.secrets;

import com.stratocloud.exceptions.StratoException;

public class SecretsManagerRegistry {

    private static SecretsManager registeredManager = null;

    public static void register(SecretsManager secretsManager){
        if(registeredManager != null && registeredManager != secretsManager)
            throw new StratoException("Multiple secrets manager detected.");

        registeredManager = secretsManager;
    }

    public static SecretsManager getSecretsManager(){
        if(registeredManager == null)
            throw new StratoException("Secrets manager not registered yet.");

        return registeredManager;
    }
}
