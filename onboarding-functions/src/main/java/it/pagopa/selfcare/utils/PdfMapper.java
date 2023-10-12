package it.pagopa.selfcare.utils;

import it.pagopa.selfcare.commons.base.utils.InstitutionType;

import it.pagopa.selfcare.entity.Billing;
import it.pagopa.selfcare.entity.Institution;
import it.pagopa.selfcare.entity.Onboarding;
import it.pagopa.selfcare.exception.GenericOnboardingException;
import it.pagopa.selfcare.onboarding.common.InstitutionPaSubunitType;
import it.pagopa.selfcare.onboarding.common.Origin;
import it.pagopa.selfcare.onboarding.common.PricingPlan;
import org.openapi.quarkus.user_registry_json.model.UserResource;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_IO;
import static it.pagopa.selfcare.utils.GenericError.MANAGER_EMAIL_NOT_FOUND;


public class PdfMapper {

    private static final String[] PLAN_LIST = {"C1", "C2", "C3", "C4", "C5", "C6", "C7"};

    public static Map<String, Object> setUpCommonData(UserResource validManager, List<UserResource> users, Institution institution, Billing billing, List<String> geographicTaxonomies) {

        //if (validManager.getWorkContacts() != null && validManager.getWorkContacts().containsKey(institution.getId())) {
        //    throw new GenericOnboardingException(MANAGER_EMAIL_NOT_FOUND.getMessage(), MANAGER_EMAIL_NOT_FOUND.getCode());
        //}

        Map<String, Object> map = new HashMap<>();
        map.put("institutionName", institution.getDescription());
        map.put("address", institution.getAddress());
        map.put("institutionTaxCode", institution.getTaxCode());
        map.put("zipCode", institution.getZipCode());
        map.put("managerName", validManager.getName());
        map.put("managerSurname", validManager.getFamilyName());
        //map.put("originId", institution.getOriginId() != null ? institution.getOriginId() : "");
        map.put("institutionMail", institution.getDigitalAddress());
        map.put("managerTaxCode", validManager.getFiscalCode());
        //map.put("managerEmail", validManager.getWorkContacts().get(institution.getId()).getEmail());
        //map.put("delegates", delegatesToText(users, institution.getId()));
        map.put("institutionType", decodeInstitutionType(institution.getInstitutionType()));
        map.put("institutionVatNumber", Optional.ofNullable(billing).map(Billing::getVatNumber).orElse(""));

        if (geographicTaxonomies != null && !geographicTaxonomies.isEmpty()) {
            map.put("institutionGeoTaxonomies", geographicTaxonomies);
        }
        if(institution.getSubunitType() != null && (institution.getSubunitType().equals(InstitutionPaSubunitType.AOO.name()) || institution.getSubunitType().equals(InstitutionPaSubunitType.UO.name()))){
            //map.put("parentInfo", " ente centrale " + institution.getParentDescription());
        } else {
            map.put("parentInfo", "");
        }
        return map;
    }

    public static void setupPSPData(Map<String, Object> map, UserResource validManager, Institution institution) {

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
        //if (validManager.getWorkContacts() != null && validManager.getWorkContacts().containsKey(institution.getId())) {
        //    map.put("managerPEC", validManager.getWorkContacts().get(institution.getId()).getEmail());
        //}
    }

    public static void setupProdIOData(Onboarding onboarding, Map<String, Object> map, UserResource validManager) {
        final Institution institution = onboarding.getInstitution();
        final InstitutionType institutionType = institution.getInstitutionType();

        map.put("institutionTypeCode", institution.getInstitutionType());
        decodePricingPlan(onboarding.getPricingPlan(), onboarding.getProductId(), map);
        if (Objects.nonNull(institution.getOrigin())) {
            map.put("originIdLabelValue", Origin.IPA.equals(institution.getOrigin()) ?
                    "<li class=\"c19 c39 li-bullet-0\"><span class=\"c1\">codice di iscrizione all&rsquo;Indice delle Pubbliche Amministrazioni e dei gestori di pubblici servizi (I.P.A.) <span class=\"c3\">${originId}</span> </span><span class=\"c1\"></span></li>"
                    : "");
        }
        addInstitutionRegisterLabelValue(institution, map);
        if (onboarding.getBilling() != null) {
            map.put("institutionRecipientCode",onboarding.getBilling().getRecipientCode());
        }

        String underscore = "_______________";
        map.put("GPSinstitutionName", InstitutionType.GSP == institutionType ? institution.getDescription() : underscore);
        map.put("GPSmanagerName", InstitutionType.GSP == institutionType ? validManager.getName() : underscore);
        map.put("GPSmanagerSurname", InstitutionType.GSP == institutionType ? validManager.getFamilyName() : underscore);
        map.put("GPSmanagerTaxCode", InstitutionType.GSP == institutionType ? validManager.getFiscalCode() : underscore);

        map.put("institutionREA", Optional.ofNullable(institution.getRea()).orElse(underscore));
        map.put("institutionShareCapital", Optional.ofNullable(institution.getShareCapital()).orElse(underscore));
        map.put("institutionBusinessRegisterPlace", Optional.ofNullable(institution.getBusinessRegisterPlace()).orElse(underscore));

        addPricingPlan(onboarding.getPricingPlan(), map);
    }

    public static void setupSAProdInteropData(Map<String, Object> map, Institution institution) {

        String underscore = "_______________";
        map.put("institutionREA", Optional.ofNullable(institution.getRea()).orElse(underscore));
        map.put("institutionShareCapital", Optional.ofNullable(institution.getShareCapital()).orElse(underscore));
        map.put("institutionBusinessRegisterPlace", Optional.ofNullable(institution.getBusinessRegisterPlace()).orElse(underscore));
        //override originId to not fill ipa code in case of SA
        if(InstitutionType.SA.equals(institution.getInstitutionType()))
            map.put("originId", underscore);
    }

    public static void setupProdPNData(Map<String, Object> map, Institution institution, Billing billing) {

        addInstitutionRegisterLabelValue(institution, map);
        if (billing != null) {
            map.put("institutionRecipientCode", billing.getRecipientCode());
        }
    }


    private static void addPricingPlan(String pricingPlan, Map<String, Object> map) {
        if (StringUtils.hasText(pricingPlan) && Arrays.stream(PLAN_LIST).anyMatch(s -> s.equalsIgnoreCase(pricingPlan))) {
            map.put("pricingPlanPremium", pricingPlan.replace("C", ""));
            map.put("pricingPlanPremiumCheckbox", "X");
        } else {
            map.put("pricingPlanPremium", "");
            map.put("pricingPlanPremiumCheckbox", "");
        }

        map.put("pricingPlanPremiumBase", Optional.ofNullable(pricingPlan).orElse(""));

        if (StringUtils.hasText(pricingPlan) && "C0".equalsIgnoreCase(pricingPlan)) {
            map.put("pricingPlanPremiumBaseCheckbox", "X");
        } else {
            map.put("pricingPlanPremiumBaseCheckbox", "");
        }
    }

    private static void addInstitutionRegisterLabelValue(Institution institution, Map<String, Object> map) {
        if (institution.getPaymentServiceProvider() != null
                && StringUtils.hasText(institution.getPaymentServiceProvider().getBusinessRegisterNumber())) {
            map.put("number", institution.getPaymentServiceProvider().getBusinessRegisterNumber());
            map.put("institutionRegisterLabelValue", "<li class=\"c19 c39 li-bullet-0\"><span class=\"c1\">codice di iscrizione all&rsquo;Indice delle Pubbliche Amministrazioni e dei gestori di pubblici servizi (I.P.A.) <span class=\"c3\">${number}</span> </span><span class=\"c1\"></span></li>\n");
        } else {
            map.put("institutionRegisterLabelValue", "");
        }
    }

    private static void decodePricingPlan(String pricingPlan, String productId, Map<String, Object> map) {
        if (PricingPlan.FA.name().equals(pricingPlan)) {
            map.put("pricingPlanFastCheckbox", "X");
            map.put("pricingPlanBaseCheckbox", "");
            map.put("pricingPlanPremiumCheckbox", "");
            map.put("pricingPlan", PricingPlan.FA.getValue());
            return;
        }
        if (PROD_IO.getValue().equalsIgnoreCase(productId)) {
            map.put("pricingPlanFastCheckbox", "");
            map.put("pricingPlanBaseCheckbox", "X");
            map.put("pricingPlanPremiumCheckbox", "");
            map.put("pricingPlan", PricingPlan.BASE.getValue());
        } else {
            map.put("pricingPlanFastCheckbox", "");
            map.put("pricingPlanBaseCheckbox", "");
            map.put("pricingPlanPremiumCheckbox", "X");
            map.put("pricingPlan", PricingPlan.PREMIUM.getValue());
        }
    }

    private static String decodeInstitutionType(InstitutionType institutionType) {
        switch (institutionType) {
            case PA:
                return "Pubblica Amministrazione";
            case GSP:
                return "Gestore di servizi pubblici";
            case PT:
                return "Partner tecnologico";
            case SCP:
                return "Società a controllo pubblico";
            case PSP:
                return "Prestatori Servizi di Pagamento";
            default:
                return "";

        }
    }

    private static String delegatesToText(List<UserResource> users, String institutionId) {
        StringBuilder builder = new StringBuilder();
        users.forEach(user -> {
            builder
                    .append("</br>")
                    .append("<p class=\"c141\"><span class=\"c6\">Nome e Cognome: ")
                    .append(user.getName()).append(" ")
                    .append(user.getFamilyName())
                    .append("&nbsp;</span></p>\n")
                    .append("<p class=\"c141\"><span class=\"c6\">Codice Fiscale: ")
                    .append(user.getFiscalCode())
                    .append("</span></p>\n")
                    .append("<p class=\"c141\"><span class=\"c6\">Amm.ne/Ente/Societ&agrave;: </span></p>\n")
                    .append("<p class=\"c141\"><span class=\"c6\">Qualifica/Posizione: </span></p>\n")
                    .append("<p class=\"c141\"><span class=\"c6\">e-mail: ");
            if (user.getWorkContacts() != null && user.getWorkContacts().containsKey(institutionId)) {
                builder.append(user.getWorkContacts().get(institutionId).getEmail());
            }
            builder.append("&nbsp;</span></p>\n")
                    .append("<p class=\"c141\"><span class=\"c6\">PEC: &nbsp;</span></p>\n")
                    .append("</br>");
        });
        return builder.toString();
    }
}
