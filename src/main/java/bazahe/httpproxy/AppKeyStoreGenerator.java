package bazahe.httpproxy;


import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.bouncycastle.jce.PrincipalUtil;
import org.bouncycastle.jce.provider.X509CertificateObject;
import org.bouncycastle.x509.extension.SubjectKeyIdentifierStructure;

import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.util.Calendar;
import java.util.Date;

/**
 * Dynamic generate self signed certificate for mitm proxy, with specified root ca private key and certificate.
 * JDK do not have an open api for this, although Open JDK/SUN JDK does have a internal api can work.
 * So we use  Bouncy Castle here.
 */
@Log4j2
public class AppKeyStoreGenerator {
    private final X509Certificate caCertificate;
    private final RSAPrivateCrtKeyParameters privateKeyParameters;


    private SecureRandom secureRandom;


    @SneakyThrows
    public AppKeyStoreGenerator(String caKeyStorePath, char[] caKeyStorePassword, String alias) {

        log.info("Loading CA certificate/private key from file {}, alias {}", caKeyStorePath, alias);
        KeyStore caKeyStore = KeyStore.getInstance("PKCS12");
        try (InputStream input = new FileInputStream(caKeyStorePath)) {
            caKeyStore.load(input, caKeyStorePassword);
        }
        Key key = caKeyStore.getKey(alias, caKeyStorePassword);
        if (key == null) {
            throw new RuntimeException("Specified key of the KeyStore not found!");
        }
        RSAPrivateCrtKey privateCrtKey = (RSAPrivateCrtKey) key;
        privateKeyParameters = new RSAPrivateCrtKeyParameters(privateCrtKey.getModulus(),
                privateCrtKey.getPublicExponent(),
                privateCrtKey.getPrivateExponent(),
                privateCrtKey.getPrimeP(), privateCrtKey.getPrimeQ(), privateCrtKey.getPrimeExponentP(),
                privateCrtKey.getPrimeExponentQ(),
                privateCrtKey.getCrtCoefficient());
        // and get the certificate
        caCertificate = (X509Certificate) caKeyStore.getCertificate(alias);
        if (caCertificate == null) {
            throw new RuntimeException("Specified certificate of the KeyStore not found!");
        }
        log.debug("Successfully loaded CA key and certificate. CA DN is {}", caCertificate.getSubjectDN().getName());
        caCertificate.verify(caCertificate.getPublicKey());
        log.debug("Successfully verified CA certificate with its own public key.");
        secureRandom = new SecureRandom();
    }

    @SneakyThrows
    public KeyStore generateKeyStore(String domain, int validityDays, char[] password) {
        log.info("Generating certificate for domain {}, valid for {} days", domain, validityDays);
        // generate the key pair for the new certificate
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048, secureRandom);
        KeyPair keypair = keyGen.generateKeyPair();
        PrivateKey privateKey = keypair.getPrivate();
        PublicKey publicKey = keypair.getPublic();

        Calendar calendar = Calendar.getInstance();
        Date startDate = calendar.getTime();
        calendar.add(Calendar.DAY_OF_YEAR, validityDays);
        Date expireDate = calendar.getTime();

        String appDName = String.format("CN=%s, OU=TianCao, O=TianCao, L=Beijing, ST=Beijing, C=CN", domain);
        X500Name x500Name = new X500Name(appDName);

        V3TBSCertificateGenerator certificateGenerator = new V3TBSCertificateGenerator();
        certificateGenerator.setSerialNumber(new DERInteger(BigInteger.valueOf(System.currentTimeMillis())));
        certificateGenerator.setIssuer(PrincipalUtil.getSubjectX509Principal(caCertificate));
        certificateGenerator.setSubject(x500Name);
        DERObjectIdentifier sigOID = PKCSObjectIdentifiers.sha256WithRSAEncryption;
        AlgorithmIdentifier sigAlgId = new AlgorithmIdentifier(sigOID, new DERNull());
        certificateGenerator.setSignature(sigAlgId);
        try (InputStream bis = new ByteArrayInputStream(publicKey.getEncoded());
             ASN1InputStream asn1InputStream = new ASN1InputStream(bis)) {
            SubjectPublicKeyInfo publicKeyInfo = new SubjectPublicKeyInfo((ASN1Sequence) asn1InputStream.readObject());
            certificateGenerator.setSubjectPublicKeyInfo(publicKeyInfo);

        }
        certificateGenerator.setStartDate(new Time(startDate));
        certificateGenerator.setEndDate(new Time(expireDate));

        // Set SubjectAlternativeName
//        X509ExtensionsGenerator x509ExtensionsGenerator = new X509ExtensionsGenerator();
//        GeneralNames subjectAltName = new GeneralNames(new GeneralName(GeneralName.dNSName, "www.v2ex.com"));
//        x509ExtensionsGenerator.addExtension(X509Extensions.SubjectAlternativeName, true, new DEROctetString
//                (subjectAltName));
//        X509Extensions x509Extensions = x509ExtensionsGenerator.generate();
//        certificateGenerator.setExtensions(x509Extensions);

        TBSCertificateStructure tbsCertificateStructure = certificateGenerator.generateTBSCertificate();

        byte[] data;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             DEROutputStream derOutputStream = new DEROutputStream(bos)) {
            derOutputStream.writeObject(tbsCertificateStructure);
            data = bos.toByteArray();
        }

        PrivateKey caPrivateKey = KeyFactory.getInstance("RSA").generatePrivate(getKeySpec());
        Signature signature = Signature.getInstance(sigOID.getId());
        signature.initSign(caPrivateKey, secureRandom);
        signature.update(data);
        byte[] signatureData = signature.sign();

        // and finally construct the certificate structure
        ASN1EncodableVector asn1EncodableVector = new ASN1EncodableVector();
        asn1EncodableVector.add(tbsCertificateStructure);
        asn1EncodableVector.add(sigAlgId);
        asn1EncodableVector.add(new DERBitString(signatureData));

        DERSequence derSequence = new DERSequence(asn1EncodableVector);
        X509CertificateStructure x509CertificateStructure = new X509CertificateStructure(derSequence);
        X509CertificateObject clientCertificate = new X509CertificateObject(x509CertificateStructure);
        log.debug("Verifying certificate for correct signature with CA public key");
        clientCertificate.verify(caCertificate.getPublicKey());
        clientCertificate.setBagAttribute(PKCSObjectIdentifiers.pkcs_9_at_friendlyName,
                new DERBMPString("Certificate for IPSec WLAN access"));
        clientCertificate.setBagAttribute(PKCSObjectIdentifiers.pkcs_9_at_localKeyId, new
                SubjectKeyIdentifierStructure(publicKey));
        KeyStore store = KeyStore.getInstance("PKCS12");
        store.load(null, null);

        X509Certificate[] chain = new X509Certificate[2];
        // first the client, then the CA certificate
        chain[0] = clientCertificate;
        chain[1] = caCertificate;

        store.setKeyEntry("bazahe app", privateKey, password, chain);
        return store;
    }

    private RSAPrivateCrtKeySpec getKeySpec() {
        return new RSAPrivateCrtKeySpec(this.privateKeyParameters.getModulus(), this.privateKeyParameters
                .getPublicExponent(),
                this.privateKeyParameters.getExponent(), this.privateKeyParameters.getP(), this
                .privateKeyParameters.getQ(),
                this.privateKeyParameters.getDP(), this.privateKeyParameters.getDQ(), this
                .privateKeyParameters.getQInv());
    }


    @SneakyThrows
    public static void main(String[] args) {
        String path = "/Users/dongliu/code/java/bazahe/certificates/root_ca.p12";
        AppKeyStoreGenerator generator = new AppKeyStoreGenerator(path, "123456".toCharArray(), "mykey");
        KeyStore appKeyStore = generator.generateKeyStore("www.v2ex.com", 365, "123456".toCharArray());
        try (FileOutputStream outputStream = new FileOutputStream("bazahe_1.p12")) {
            appKeyStore.store(outputStream, "123456".toCharArray());
        }
    }
}
