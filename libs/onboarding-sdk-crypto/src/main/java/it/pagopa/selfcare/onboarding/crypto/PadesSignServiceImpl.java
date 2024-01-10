package it.pagopa.selfcare.onboarding.crypto;

import it.pagopa.selfcare.onboarding.crypto.entity.SignatureInformation;
import it.pagopa.selfcare.onboarding.crypto.utils.CryptoUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;

public class PadesSignServiceImpl implements PadesSignService {
    private final Pkcs7HashSignService pkcs7Signature;

    public PadesSignServiceImpl(Pkcs7HashSignService pkcs7Signature) {
        this.pkcs7Signature = pkcs7Signature;
    }

    public void padesSign(File pdfFile, File signedPdfFile, SignatureInformation signInfo) {
        CryptoUtils.createParentDirectoryIfNotExists(signedPdfFile);

        try (FileOutputStream fos = new FileOutputStream(signedPdfFile);
                PDDocument doc = PDDocument.load(pdfFile)){

            PDSignature signature = new PDSignature();
            signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
            signature.setName(signInfo.getName());
            signature.setLocation(signInfo.getLocation());
            signature.setReason(signInfo.getReason());
            signature.setSignDate(Calendar.getInstance());
            SignatureOptions signatureOptions = new SignatureOptions();
            signatureOptions.setPreferredSignatureSize(35944);
            doc.addSignature(signature, this.pkcs7Signature, signatureOptions);
            doc.saveIncremental(fos);

        } catch (Exception var12) {
            throw new IllegalStateException(String.format("Something gone wrong while signing input pdf %s and storing it into %s", pdfFile.getAbsolutePath(), signedPdfFile.getAbsolutePath()), var12);
        }
    }
}

