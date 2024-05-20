package it.pagopa.selfcare.onboarding.utils;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.Origin;
import it.pagopa.selfcare.onboarding.common.PricingPlan;
import it.pagopa.selfcare.onboarding.entity.*;
import it.pagopa.selfcare.onboarding.exception.GenericOnboardingException;
import org.openapi.quarkus.user_registry_json.model.CertifiableFieldResourceOfstring;
import org.openapi.quarkus.user_registry_json.model.UserResource;

import java.util.*;

import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_IO;
import static it.pagopa.selfcare.onboarding.utils.GenericError.MANAGER_EMAIL_NOT_FOUND;


public class PdfMapper {

    private static final String UNDERSCORE = "_______________";
    private static final String[] PLAN_LIST = {"C1", "C2", "C3", "C4", "C5", "C6", "C7"};
    public static final String INSTITUTION_REA = "institutionREA";
    public static final String INSTITUTION_NAME = "institutionName";
    public static final String INSTITUTION_SHARE_CAPITAL = "institutionShareCapital";
    public static final String INSTITUTION_BUSINESS_REGISTER_PLACE = "institutionBusinessRegisterPlace";
    public static final String PRICING_PLAN_PREMIUM = "pricingPlanPremium";
    public static final String PRICING_PLAN_PREMIUM_CHECKBOX = "pricingPlanPremiumCheckbox";
    public static final String PRICING_PLAN_FAST_CHECKBOX = "pricingPlanFastCheckbox";
    public static final String PRICING_PLAN_BASE_CHECKBOX = "pricingPlanBaseCheckbox";
    public static final String PRICING_PLAN = "pricingPlan";
    public static final String INSTITUTION_REGISTER_LABEL_VALUE = "institutionRegisterLabelValue";
    public static final String ORIGIN_ID_LABEL = "<li class=\"c19 c39 li-bullet-0\"><span class=\"c1\">codice di iscrizione all&rsquo;Indice delle Pubbliche Amministrazioni e dei gestori di pubblici servizi (I.P.A.) <span class=\"c3\">${originId}</span> </span><span class=\"c1\"></span></li>";

    private PdfMapper() {
    }

    public static Map<String, Object> setUpCommonData(UserResource manager, List<UserResource> users, Onboarding onboarding) {

        Institution institution = onboarding.getInstitution();
        Billing billing = onboarding.getBilling();
        User userManager = onboarding.getUsers().stream()
                .filter(user -> user.getId().equals(manager.getId().toString()))
                .findFirst()
                .orElseThrow();

        List<String> geographicTaxonomies = Optional.ofNullable(onboarding.getInstitution().getGeographicTaxonomies())
                .map(geoTaxonomies -> geoTaxonomies.stream().map(GeographicTaxonomy::getDesc).toList())
                .orElse(List.of());

        String mailManager = getMailManager(manager, userManager.getUserMailUuid());
        if (Objects.isNull(mailManager)) {
            throw new GenericOnboardingException(MANAGER_EMAIL_NOT_FOUND.getMessage(), MANAGER_EMAIL_NOT_FOUND.getCode());
        }

        Map<String, Object> map = new HashMap<>();
        map.put(INSTITUTION_NAME, institution.getDescription());
        map.put("address", institution.getAddress());
        map.put("institutionTaxCode", Optional.ofNullable(institution.getTaxCode()).orElse(UNDERSCORE));
        if (Objects.nonNull(institution.getCountry()) && !institution.getCountry().equals("IT")) {
            map.put("extCountry", institution.getCity() + " (" + institution.getCountry() + ")");
        } else {
            map.put("extCountry", "");
        }
        map.put("zipCode", Optional.ofNullable(institution.getZipCode()).orElse(""));
        map.put("managerName", getStringValue(manager.getName()));
        map.put("managerSurname", getStringValue(manager.getFamilyName()));
        map.put("originId", Optional.ofNullable(institution.getOriginId()).orElse(UNDERSCORE));
        map.put("institutionMail", institution.getDigitalAddress());
        map.put("managerTaxCode", manager.getFiscalCode());
        map.put("managerEmail", mailManager);
        map.put("delegates", delegatesToText(users, onboarding.getUsers()));
        map.put("institutionType", decodeInstitutionType(institution.getInstitutionType()));
        map.put("institutionVatNumber", Optional.ofNullable(billing).map(Billing::getVatNumber).orElse(UNDERSCORE));
        map.put("taxCodeInvoicing", Optional.ofNullable(billing).map(Billing::getTaxCodeInvoicing).orElse(UNDERSCORE));

        if (!geographicTaxonomies.isEmpty()) {
            map.put("institutionGeoTaxonomies", geographicTaxonomies);
        }

        map.put("parentInfo", Objects.nonNull(institution.getParentDescription()) ? " ente centrale " + institution.getParentDescription() : "");
        return map;
    }

    private static String getMailManager(UserResource manager, String userMailUuid){
        if(Objects.isNull(manager.getWorkContacts())
                || !manager.getWorkContacts().containsKey(userMailUuid)) {
            return null;
        }

        return Optional.ofNullable(manager.getWorkContacts()
                        .get(userMailUuid)
                        .getEmail())
                .map(CertifiableFieldResourceOfstring::getValue)
                .orElse(null);
    }

    public static void setupPSPData(Map<String, Object> map, UserResource validManager, Onboarding onboarding) {
        Institution institution = onboarding.getInstitution();
        if (institution.getPaymentServiceProvider() != null) {
            map.put("legalRegisterNumber", institution.getPaymentServiceProvider().getLegalRegisterNumber());
            map.put("legalRegisterName", institution.getPaymentServiceProvider().getLegalRegisterName());
            map.put("vatNumberGroup", institution.getPaymentServiceProvider().isVatNumberGroup() ? "partita iva di gruppo" : "");
            map.put("institutionRegister", institution.getPaymentServiceProvider().getBusinessRegisterNumber());
            map.put("institutionAbi", institution.getPaymentServiceProvider().getAbiCode());
        }
        if (institution.getDataProtectionOfficer() != null) {
            map.put("dataProtectionOfficerAddress", institution.getDataProtectionOfficer().getAddress());
            map.put("dataProtectionOfficerEmail", institution.getDataProtectionOfficer().getEmail());
            map.put("dataProtectionOfficerPec", institution.getDataProtectionOfficer().getPec());
        }

        /* set manager PEC */
        onboarding.getUsers().stream()
                .filter(user -> validManager.getId().toString().equals(user.getId()))
                .map(User::getUserMailUuid)
                .findFirst()
                .map(userMailUuid -> getMailManager(validManager, userMailUuid))
                .ifPresent(mail -> map.put("managerPEC", mail));
    }

    public static void setECData(Map<String, Object> map, Onboarding onboarding) {
        Institution institution = onboarding.getInstitution();
        map.put(INSTITUTION_REA, Optional.ofNullable(institution.getRea()).orElse(UNDERSCORE));
        map.put(INSTITUTION_SHARE_CAPITAL, Optional.ofNullable(institution.getShareCapital()).orElse(UNDERSCORE));
        map.put(INSTITUTION_BUSINESS_REGISTER_PLACE, Optional.ofNullable(institution.getBusinessRegisterPlace()).orElse(UNDERSCORE));
    }

    public static void setupProdIOData(Onboarding onboarding, Map<String, Object> map, UserResource validManager) {
        final Institution institution = onboarding.getInstitution();
        final InstitutionType institutionType = institution.getInstitutionType();

        map.put("institutionTypeCode", institution.getInstitutionType());
        decodePricingPlan(onboarding.getPricingPlan(), onboarding.getProductId(), map);

        map.put("originIdLabelValue", Origin.IPA.equals(institution.getOrigin()) ? ORIGIN_ID_LABEL : "");

        addInstitutionRegisterLabelValue(institution, map);
        if (onboarding.getBilling() != null) {
            map.put("institutionRecipientCode",onboarding.getBilling().getRecipientCode());
        }

        map.put("GPSinstitutionName", InstitutionType.GSP == institutionType ? institution.getDescription() : UNDERSCORE);
        map.put("GPSmanagerName", InstitutionType.GSP == institutionType ? getStringValue(validManager.getName()) : UNDERSCORE);
        map.put("GPSmanagerSurname", InstitutionType.GSP == institutionType ? getStringValue(validManager.getFamilyName()) : UNDERSCORE);
        map.put("GPSmanagerTaxCode", InstitutionType.GSP == institutionType ? validManager.getFiscalCode() : UNDERSCORE);

        map.put(INSTITUTION_REA, Optional.ofNullable(institution.getRea()).orElse(UNDERSCORE));
        map.put(INSTITUTION_SHARE_CAPITAL, Optional.ofNullable(institution.getShareCapital()).orElse(UNDERSCORE));
        map.put(INSTITUTION_BUSINESS_REGISTER_PLACE, Optional.ofNullable(institution.getBusinessRegisterPlace()).orElse(UNDERSCORE));

        addPricingPlan(onboarding.getPricingPlan(), map);
    }

    public static void setupSAProdInteropData(Map<String, Object> map, Institution institution) {

        map.put(INSTITUTION_REA, Optional.ofNullable(institution.getRea()).orElse(UNDERSCORE));
        map.put(INSTITUTION_SHARE_CAPITAL, Optional.ofNullable(institution.getShareCapital()).orElse(UNDERSCORE));
        map.put(INSTITUTION_BUSINESS_REGISTER_PLACE, Optional.ofNullable(institution.getBusinessRegisterPlace()).orElse(UNDERSCORE));
        //override originId to not fill ipa code in case of SA
        if(InstitutionType.SA.equals(institution.getInstitutionType()))
            map.put("originId", UNDERSCORE);
    }

    public static void setupProdPNData(Map<String, Object> map, Institution institution, Billing billing) {

        addInstitutionRegisterLabelValue(institution, map);
        if (billing != null) {
            map.put("institutionRecipientCode", billing.getRecipientCode());
        }
    }


    private static void addPricingPlan(String pricingPlan, Map<String, Object> map) {
        if (Objects.nonNull(pricingPlan) && Arrays.stream(PLAN_LIST).anyMatch(s -> s.equalsIgnoreCase(pricingPlan))) {
            map.put(PRICING_PLAN_PREMIUM, pricingPlan.replace("C", ""));
            map.put(PRICING_PLAN_PREMIUM_CHECKBOX, "X");
        } else {
            map.put(PRICING_PLAN_PREMIUM, "");
            map.put(PRICING_PLAN_PREMIUM_CHECKBOX, "");
        }

        map.put("pricingPlanPremiumBase", Optional.ofNullable(pricingPlan).orElse(""));

        if (Objects.nonNull(pricingPlan) && "C0".equalsIgnoreCase(pricingPlan)) {
            map.put(PRICING_PLAN_PREMIUM_CHECKBOX, "X");
        } else {
            map.put(PRICING_PLAN_PREMIUM_CHECKBOX, "");
        }
    }

    private static void addInstitutionRegisterLabelValue(Institution institution, Map<String, Object> map) {
        if (institution.getPaymentServiceProvider() != null
                && Objects.nonNull(institution.getPaymentServiceProvider().getBusinessRegisterNumber())) {
            map.put("number", institution.getPaymentServiceProvider().getBusinessRegisterNumber());
            map.put(INSTITUTION_REGISTER_LABEL_VALUE, "<li class=\"c19 c39 li-bullet-0\"><span class=\"c1\">codice di iscrizione all&rsquo;Indice delle Pubbliche Amministrazioni e dei gestori di pubblici servizi (I.P.A.) <span class=\"c3\">${number}</span> </span><span class=\"c1\"></span></li>\n");
        } else {
            map.put(INSTITUTION_REGISTER_LABEL_VALUE, "");
        }
    }

    private static void decodePricingPlan(String pricingPlan, String productId, Map<String, Object> map) {
        if (PricingPlan.FA.name().equals(pricingPlan)) {
            map.put(PRICING_PLAN_FAST_CHECKBOX, "X");
            map.put(PRICING_PLAN_BASE_CHECKBOX, "");
            map.put(PRICING_PLAN_PREMIUM_CHECKBOX, "");
            map.put(PRICING_PLAN, PricingPlan.FA.getValue());
            return;
        }
        if (PROD_IO.getValue().equalsIgnoreCase(productId)) {
            map.put(PRICING_PLAN_FAST_CHECKBOX, "");
            map.put(PRICING_PLAN_BASE_CHECKBOX, "X");
            map.put(PRICING_PLAN_PREMIUM_CHECKBOX, "");
            map.put(PRICING_PLAN, PricingPlan.BASE.getValue());
        } else {
            map.put(PRICING_PLAN_FAST_CHECKBOX, "");
            map.put(PRICING_PLAN_BASE_CHECKBOX, "");
            map.put(PRICING_PLAN_PREMIUM_CHECKBOX, "X");
            map.put(PRICING_PLAN, PricingPlan.PREMIUM.getValue());
        }
    }

    private static String decodeInstitutionType(InstitutionType institutionType) {
        return switch (institutionType) {
            case PA -> "Pubblica Amministrazione";
            case GSP -> "Gestore di servizi pubblici";
            case PT -> "Partner tecnologico";
            case SCP -> "Società a controllo pubblico";
            case PSP -> "Prestatori Servizi di Pagamento";
            default -> "";
        };
    }

    private static String delegatesToText(List<UserResource> userResources, List<User> users) {
        StringBuilder builder = new StringBuilder();
        userResources.forEach(userResource -> {
            builder
                    .append("</br>")
                    .append("<p class=\"c141\"><span class=\"c6\">Nome e Cognome: ")
                    .append(getStringValue(userResource.getName())).append(" ")
                    .append(getStringValue(userResource.getFamilyName()))
                    .append("&nbsp;</span></p>\n")
                    .append("<p class=\"c141\"><span class=\"c6\">Codice Fiscale: ")
                    .append(userResource.getFiscalCode())
                    .append("</span></p>\n")
                    .append("<p class=\"c141\"><span class=\"c6\">Amm.ne/Ente/Societ&agrave;: </span></p>\n")
                    .append("<p class=\"c141\"><span class=\"c6\">Qualifica/Posizione: </span></p>\n")
                    .append("<p class=\"c141\"><span class=\"c6\">e-mail: ");

            users.stream()
                    .filter(user -> userResource.getId().toString().equals(user.getId()))
                    .map(User::getUserMailUuid)
                    .findFirst()
                    .filter(userMailUuid -> Objects.nonNull(userResource.getWorkContacts()) &&
                            userResource.getWorkContacts().containsKey(userMailUuid))
                    .ifPresent(userMailUuid ->
                        builder.append(getStringValue(userResource.getWorkContacts().get(userMailUuid).getEmail())));


            builder.append("&nbsp;</span></p>\n")
                    .append("<p class=\"c141\"><span class=\"c6\">PEC: &nbsp;</span></p>\n")
                    .append("</br>");
        });
        return builder.toString();
    }

    private static String getStringValue(CertifiableFieldResourceOfstring resourceOfString) {
        return Optional.ofNullable(resourceOfString).map(CertifiableFieldResourceOfstring::getValue).orElse("");
    }
}
