package fr.gouv.clea.integrationtests.model;

import fr.inria.clea.lsp.Location;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;

@Value
@RequiredArgsConstructor
public class Place {

    Location location;

    Location staffLocation;

    List<DeepLink> locationDeepLinks = new ArrayList<>();

    List<DeepLink> locationStaffDeepLinks = new ArrayList<>();

    public void addDeepLink(final DeepLink deepLink) {
        locationDeepLinks.add(deepLink);
    }

    public void addStaffDeepLink(final DeepLink deepLink) {
        locationStaffDeepLinks.add(deepLink);
    }
}
