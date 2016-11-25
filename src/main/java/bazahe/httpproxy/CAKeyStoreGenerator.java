package bazahe.httpproxy;

import bazahe.ui.Constants;
import lombok.Getter;
import lombok.SneakyThrows;
import net.dongliu.commons.codec.Base64s;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.bc.BcX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;

/**
 * Generate ca root key store
 *
 * @author Liu Dong
 */
public class CAKeyStoreGenerator {

    // key store data of p#12 format
    @Getter
    private byte[] keyStoreData;
    // X.509 Certificate in der format
    @Getter
    private byte[] derCertData;
    // X.509 Certificate in pem format
    @Getter
    private byte[] pemCertData;

    @SneakyThrows
    public void generate(char[] password, int validityDays) {
        SecureRandom secureRandom = new SecureRandom();
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048, secureRandom);
        KeyPair keypair = keyGen.generateKeyPair();
        PrivateKey privateKey = keypair.getPrivate();
        PublicKey publicKey = keypair.getPublic();

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);

        Security.addProvider(new BouncyCastleProvider());

        String appDName = "CN=Love ISummer, OU=TianCao, O=TianCao, L=Beijing, ST=Beijing, C=CN";
        X500Name issuerName = new X500Name(appDName);
        X500Name subjectName = new X500Name(appDName);
        Calendar calendar = Calendar.getInstance();
        Date startDate = calendar.getTime();
        calendar.add(Calendar.DAY_OF_YEAR, validityDays);
        Date endDate = calendar.getTime();


        byte[] encoded = publicKey.getEncoded();
        SubjectPublicKeyInfo subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(ASN1Sequence.getInstance(encoded));
        X509v3CertificateBuilder builder = new X509v3CertificateBuilder(issuerName,
                BigInteger.valueOf(secureRandom.nextLong() + System.currentTimeMillis()),
                startDate, endDate,
                subjectName,
                subjectPublicKeyInfo
        );

        builder.addExtension(Extension.subjectKeyIdentifier, false, createSubjectKeyIdentifier(publicKey));
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        KeyUsage usage = new KeyUsage(KeyUsage.keyCertSign | KeyUsage.digitalSignature | KeyUsage.keyEncipherment
                | KeyUsage.dataEncipherment | KeyUsage.cRLSign);
        builder.addExtension(Extension.keyUsage, false, usage);

        ASN1EncodableVector purposes = new ASN1EncodableVector();
        purposes.add(KeyPurposeId.id_kp_serverAuth);
        purposes.add(KeyPurposeId.id_kp_clientAuth);
        purposes.add(KeyPurposeId.anyExtendedKeyUsage);
        builder.addExtension(Extension.extendedKeyUsage, false, new DERSequence(purposes));

        X509Certificate cert = signCertificate(builder, privateKey);
        cert.checkValidity(new Date());
        cert.verify(publicKey);

        export(cert);

        X509Certificate[] chain = new X509Certificate[]{cert};

        keyStore.setEntry("bazahe", new KeyStore.PrivateKeyEntry(privateKey, chain),
                new KeyStore.PasswordProtection("123456".toCharArray()));
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            keyStore.store(bos, password);
            this.keyStoreData = bos.toByteArray();
        }
    }

    @SneakyThrows
    private void export(X509Certificate cert) {
        byte[] data = cert.getEncoded();
        this.derCertData = data;
        this.pemCertData = ("-----BEGIN CERTIFICATE-----\n" +
                Base64s.mime().encode(data).toBase64String() +
                "\n-----END CERTIFICATE-----\n").getBytes(StandardCharsets.US_ASCII);
    }

    private static SubjectKeyIdentifier createSubjectKeyIdentifier(Key key) throws IOException {
        try (ASN1InputStream is = new ASN1InputStream(new ByteArrayInputStream(key.getEncoded()))) {
            ASN1Sequence seq = (ASN1Sequence) is.readObject();
            SubjectPublicKeyInfo info = SubjectPublicKeyInfo.getInstance(seq);
            return new BcX509ExtensionUtils().createSubjectKeyIdentifier(info);
        }
    }

    private static X509Certificate signCertificate(X509v3CertificateBuilder certificateBuilder,
                                                   PrivateKey signedWithPrivateKey)
            throws OperatorCreationException, CertificateException {
        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSAEncryption")
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .build(signedWithPrivateKey);
        return new JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .getCertificate(certificateBuilder.build(signer));
    }

    @SneakyThrows
    public static void main(String[] args) {
        CAKeyStoreGenerator generator = new CAKeyStoreGenerator();
        generator.generate(Constants.keyStorePassword, Constants.rootCertificateValidates);
        Path write = Files.write(Paths.get("root_ca.p12"), generator.getKeyStoreData());
        System.out.println(write);
    }
}
