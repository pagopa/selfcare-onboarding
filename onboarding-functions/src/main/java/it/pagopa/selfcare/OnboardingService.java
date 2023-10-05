package it.pagopa.selfcare;

import it.pagopa.selfcare.commons.base.security.PartyRole;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.File;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@ApplicationScoped
public class OnboardingService {
    public String greeting(String name) {
        return "Guten Tag " + name;
    }
}
