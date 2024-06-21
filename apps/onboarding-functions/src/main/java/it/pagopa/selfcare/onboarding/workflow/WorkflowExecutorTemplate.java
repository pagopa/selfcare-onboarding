package it.pagopa.selfcare.onboarding.workflow;

import it.pagopa.selfcare.onboarding.config.MailTemplatePathConfig;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.product.entity.Product;

public interface WorkflowExecutorTemplate extends WorkflowExecutor {

   String getContractTemplatePath(Product product, Onboarding onboarding);
   String getEmailRegistrationPath(MailTemplatePathConfig mailTemplatePathConfig);
   String getEmailCompletionPath();
}
