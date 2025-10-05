package com.candyrealms.candycore.modules.staff.modules;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class FreezeModule {

    private final Set<UUID> frozenUUIDS = new HashSet<>();

    public void addFrozenUUID(UUID uuid) {
        frozenUUIDS.add(uuid);
    }

    public void removeFrozenUUID(UUID uuid) {
        frozenUUIDS.remove(uuid);
    }

    public boolean isUUIDFrozen(UUID uuid) {
        return frozenUUIDS.contains(uuid);
    }
}
