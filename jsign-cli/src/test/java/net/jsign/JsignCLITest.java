/**
 * Copyright 2012 Emmanuel Bourg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.jsign;

import java.io.File;
import java.net.ProxySelector;
import java.security.InvalidParameterException;
import java.security.Permission;
import java.security.ProviderException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.netty.handler.codec.http.HttpRequest;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.bouncycastle.cms.CMSSignedData;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.ProxyAuthenticator;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

import net.jsign.msi.MSIFile;
import net.jsign.pe.PEFile;
import net.jsign.script.PowerShellScript;

import static org.junit.Assert.*;

public class JsignCLITest {

    private JsignCLI cli;
    private File sourceFile = new File("target/test-classes/wineyes.exe");
    private File targetFile = new File("target/test-classes/wineyes-signed-with-cli.exe");
    
    private String keystore = "keystore.jks";
    private String alias    = "test";
    private String keypass  = "password";

    private static final long SOURCE_FILE_CRC32 = 0xA6A363D8L;

    @Before
    public void setUp() throws Exception {
        cli = new JsignCLI();
        
        // remove the files signed previously
        if (targetFile.exists()) {
            assertTrue("Unable to remove the previously signed file", targetFile.delete());
        }
        
        assertEquals("Source file CRC32", SOURCE_FILE_CRC32, FileUtils.checksumCRC32(sourceFile));
        Thread.sleep(100);
        FileUtils.copyFile(sourceFile, targetFile);
    }

    @After
    public void tearDown() {
        // reset the proxy configuration
        ProxySelector.setDefault(null);
    }

    @Test
    public void testPrintHelp() throws Exception {
        JsignCLI.main("--help");
    }

    @Test(expected = SignerException.class)
    public void testMissingKeyStore() throws Exception {
        cli.execute("" + targetFile);
    }

    @Test(expected = SignerException.class)
    public void testUnsupportedKeyStoreType() throws Exception  {
        cli.execute("--keystore=keystore.jks", "--storetype=ABC", "" + targetFile);
    }

    @Test(expected = SignerException.class)
    public void testKeyStoreNotFound() throws Exception  {
        cli.execute("--keystore=keystore2.jks", "" + targetFile);
    }

    @Test(expected = SignerException.class)
    public void testCorruptedKeyStore() throws Exception  {
        cli.execute("--keystore=" + targetFile, "" + targetFile);
    }

    @Test(expected = SignerException.class)
    public void testMissingAlias() throws Exception  {
        cli.execute("--keystore=target/test-classes/keystores/keystore.jks", "" + targetFile);
    }

    @Test(expected = SignerException.class)
    public void testAliasNotFound() throws Exception  {
        cli.execute("--keystore=target/test-classes/keystores/keystore.jks", "--alias=unknown", "" + targetFile);
    }

    @Test(expected = SignerException.class)
    public void testCertificateNotFound() throws Exception  {
        cli.execute("--keystore=target/test-classes/keystores/keystore.jks", "--alias=foo", "" + targetFile);
    }

    @Test(expected = SignerException.class)
    public void testMissingFile() throws Exception  {
        cli.execute("--keystore=target/test-classes/keystores/keystore.jks", "--alias=test", "--keypass=password");
    }

    @Test(expected = SignerException.class)
    public void testFileNotFound() throws Exception  {
        cli.execute("--keystore=target/test-classes/keystores/keystore.jks", "--alias=test", "--keypass=password", "wineyes-foo.exe");
    }

    @Test(expected = SignerException.class)
    public void testCorruptedFile() throws Exception  {
        cli.execute("--keystore=target/test-classes/keystores/keystore.jks", "--alias=test", "--keypass=password", "target/test-classes/keystore.jks");
    }

    @Test(expected = SignerException.class)
    public void testConflictingAttributes() throws Exception  {
        cli.execute("--keystore=target/test-classes/keystores/keystore.jks", "--alias=test", "--keypass=password", "--keyfile=privatekey.pvk", "--certfile=jsign-test-certificate-full-chain.spc", "" + targetFile);
    }

    @Test(expected = SignerException.class)
    public void testMissingCertFile() throws Exception  {
        cli.execute("--keyfile=target/test-classes/keystores/privatekey.pvk", "" + targetFile);
    }

    @Test(expected = SignerException.class)
    public void testMissingKeyFile() throws Exception  {
        cli.execute("--certfile=target/test-classes/keystores/jsign-test-certificate-full-chain.spc", "" + targetFile);
    }

    @Test(expected = SignerException.class)
    public void testCertFileNotFound() throws Exception  {
        cli.execute("--certfile=target/test-classes/keystores/certificate2.spc", "--keyfile=target/test-classes/privatekey.pvk", "" + targetFile);
    }

    @Test(expected = SignerException.class)
    public void testKeyFileNotFound() throws Exception  {
        cli.execute("--certfile=target/test-classes/keystores/jsign-test-certificate-full-chain.spc", "--keyfile=target/test-classes/privatekey2.pvk", "" + targetFile);
    }

    @Test(expected = SignerException.class)
    public void testCorruptedCertFile() throws Exception  {
        cli.execute("--certfile=target/test-classes/keystores/privatekey.pvk", "--keyfile=target/test-classes/privatekey.pvk", "" + targetFile);
    }

    @Test(expected = SignerException.class)
    public void testCorruptedKeyFile() throws Exception  {
        cli.execute("--certfile=target/test-classes/keystores/jsign-test-certificate-full-chain.spc", "--keyfile=target/test-classes/jsign-test-certificate-full-chain.spc", "" + targetFile);
    }

    @Test(expected = SignerException.class)
    public void testUnsupportedDigestAlgorithm() throws Exception  {
        cli.execute("--alg=SHA-123", "--keystore=target/test-classes/keystores/keystore.jks", "--alias=test", "--keypass=password", "" + targetFile);
    }

    @Test
    public void testSigning() throws Exception {
        cli.execute("--name=WinEyes", "--url=http://www.steelblue.com/WinEyes", "--alg=SHA-1", "--keystore=target/test-classes/keystores/" + keystore, "--alias=" + alias, "--keypass=" + keypass, "" + targetFile);

        assertTrue("The file " + targetFile + " wasn't changed", SOURCE_FILE_CRC32 != FileUtils.checksumCRC32(targetFile));

        try (PEFile peFile = new PEFile(targetFile)) {
            List<CMSSignedData> signatures = peFile.getSignatures();
            assertNotNull(signatures);
            assertEquals(1, signatures.size());

            CMSSignedData signature = signatures.get(0);

            assertNotNull(signature);
        }
    }

    @Test
    public void testSigningPowerShell() throws Exception {
        File sourceFile = new File("target/test-classes/hello-world.ps1");
        File targetFile = new File("target/test-classes/hello-world-signed-with-cli.ps1");
        FileUtils.copyFile(sourceFile, targetFile);
        
        cli.execute("--alg=SHA-1", "--replace", "--encoding=ISO-8859-1", "--keystore=target/test-classes/keystores/" + keystore, "--alias=" + alias, "--keypass=" + keypass, "" + targetFile);

        PowerShellScript script = new PowerShellScript(targetFile);
        
        List<CMSSignedData> signatures = script.getSignatures();
        assertNotNull(signatures);
        assertEquals(1, signatures.size());

        CMSSignedData signature = signatures.get(0);

        assertNotNull(signature);
    }

    @Test
    public void testSigningPowerShellWithDefaultEncoding() throws Exception {
        File sourceFile = new File("target/test-classes/hello-world.ps1");
        File targetFile = new File("target/test-classes/hello-world-signed-with-cli.ps1");
        FileUtils.copyFile(sourceFile, targetFile);
        
        cli.execute("--alg=SHA-1", "--replace", "--keystore=target/test-classes/keystores/" + keystore, "--alias=" + alias, "--keypass=" + keypass, "" + targetFile);

        PowerShellScript script = new PowerShellScript(targetFile);
        
        List<CMSSignedData> signatures = script.getSignatures();
        assertNotNull(signatures);
        assertEquals(1, signatures.size());

        CMSSignedData signature = signatures.get(0);

        assertNotNull(signature);
    }

    @Test
    public void testSigningMSI() throws Exception {
        File sourceFile = new File("target/test-classes/minimal.msi");
        File targetFile = new File("target/test-classes/minimal-signed-with-cli.msi");
        FileUtils.copyFile(sourceFile, targetFile);
        
        cli.execute("--alg=SHA-1", "--replace", "--keystore=target/test-classes/keystores/" + keystore, "--alias=" + alias, "--keypass=" + keypass, "" + targetFile);

        try (MSIFile file = new MSIFile(targetFile)) {
            List<CMSSignedData> signatures = file.getSignatures();
            assertNotNull(signatures);
            assertEquals(1, signatures.size());

            CMSSignedData signature = signatures.get(0);

            assertNotNull(signature);
        }
    }

    @Test
    public void testSigningPKCS12() throws Exception {
        cli.execute("--name=WinEyes", "--url=http://www.steelblue.com/WinEyes", "--alg=SHA-256", "--keystore=target/test-classes/keystores/keystore.p12", "--alias=test", "--storepass=password", "" + targetFile);
        
        assertTrue("The file " + targetFile + " wasn't changed", SOURCE_FILE_CRC32 != FileUtils.checksumCRC32(targetFile));

        try (PEFile peFile = new PEFile(targetFile)) {
            List<CMSSignedData> signatures = peFile.getSignatures();
            assertNotNull(signatures);
            assertEquals(1, signatures.size());

            CMSSignedData signature = signatures.get(0);

            assertNotNull(signature);
        }
    }

    @Test
    public void testSigningPVKSPC() throws Exception {
        cli.execute("--url=http://www.steelblue.com/WinEyes", "--certfile=target/test-classes/keystores/jsign-test-certificate-full-chain.spc", "--keyfile=target/test-classes/keystores/privatekey-encrypted.pvk", "--storepass=password", "" + targetFile);
        
        assertTrue("The file " + targetFile + " wasn't changed", SOURCE_FILE_CRC32 != FileUtils.checksumCRC32(targetFile));

        try (PEFile peFile = new PEFile(targetFile)) {
            List<CMSSignedData> signatures = peFile.getSignatures();
            assertNotNull(signatures);
            assertEquals(1, signatures.size());

            CMSSignedData signature = signatures.get(0);

            assertNotNull(signature);
        }
    }

    @Test
    public void testSigningPEM() throws Exception {
        cli.execute("--certfile=target/test-classes/keystores/jsign-test-certificate.pem", "--keyfile=target/test-classes/keystores/privatekey.pkcs8.pem", "--keypass=password", "" + targetFile);
        
        assertTrue("The file " + targetFile + " wasn't changed", SOURCE_FILE_CRC32 != FileUtils.checksumCRC32(targetFile));

        try (PEFile peFile = new PEFile(targetFile)) {
            List<CMSSignedData> signatures = peFile.getSignatures();
            assertNotNull(signatures);
            assertEquals(1, signatures.size());

            CMSSignedData signature = signatures.get(0);

            assertNotNull(signature);
        }
    }

    @Test
    public void testSigningEncryptedPEM() throws Exception {
        cli.execute("--certfile=target/test-classes/keystores/jsign-test-certificate.pem", "--keyfile=target/test-classes/keystores/privatekey-encrypted.pkcs1.pem", "--keypass=password", "" + targetFile);
        
        assertTrue("The file " + targetFile + " wasn't changed", SOURCE_FILE_CRC32 != FileUtils.checksumCRC32(targetFile));

        try (PEFile peFile = new PEFile(targetFile)) {
            List<CMSSignedData> signatures = peFile.getSignatures();
            assertNotNull(signatures);
            assertEquals(1, signatures.size());

            CMSSignedData signature = signatures.get(0);

            assertNotNull(signature);
        }
    }

    @Test
    public void testTimestampingAuthenticode() throws Exception {
        File targetFile2 = new File("target/test-classes/wineyes-timestamped-with-cli-authenticode.exe");
        FileUtils.copyFile(sourceFile, targetFile2);
        cli.execute("--keystore=target/test-classes/keystores/" + keystore, "--alias=" + alias, "--keypass=" + keypass, "--tsaurl=http://timestamp.comodoca.com/authenticode", "--tsmode=authenticode", "" + targetFile2);
        
        assertTrue("The file " + targetFile2 + " wasn't changed", SOURCE_FILE_CRC32 != FileUtils.checksumCRC32(targetFile2));

        try (PEFile peFile = new PEFile(targetFile2)) {
            List<CMSSignedData> signatures = peFile.getSignatures();
            assertNotNull(signatures);
            assertEquals(1, signatures.size());
            
            CMSSignedData signature = signatures.get(0);
            
            assertNotNull(signature);
        }
    }

    @Test
    public void testTimestampingRFC3161() throws Exception {
        File targetFile2 = new File("target/test-classes/wineyes-timestamped-with-cli-rfc3161.exe");
        FileUtils.copyFile(sourceFile, targetFile2);
        cli.execute("--keystore=target/test-classes/keystores/" + keystore, "--alias=" + alias, "--keypass=" + keypass, "--tsaurl=http://timestamp.comodoca.com/rfc3161", "--tsmode=rfc3161", "" + targetFile2);

        assertTrue("The file " + targetFile2 + " wasn't changed", SOURCE_FILE_CRC32 != FileUtils.checksumCRC32(targetFile2));

        try (PEFile peFile = new PEFile(targetFile2)) {
            List<CMSSignedData> signatures = peFile.getSignatures();
            assertNotNull(signatures);
            assertEquals(1, signatures.size());

            CMSSignedData signature = signatures.get(0);

            assertNotNull(signature);
        }
    }

    @Test
    public void testTimestampingWithProxyUnauthenticated() throws Exception {
        final AtomicBoolean proxyUsed = new AtomicBoolean(false);
        HttpProxyServer proxy = DefaultHttpProxyServer.bootstrap().withPort(12543)
                .withFiltersSource(new HttpFiltersSourceAdapter() {
                    @Override
                    public HttpFilters filterRequest(HttpRequest originalRequest) {
                        proxyUsed.set(true);
                        return super.filterRequest(originalRequest);
                    }
                })
                .start();
        
        try {
            File targetFile2 = new File("target/test-classes/wineyes-timestamped-with-cli-rfc3161-proxy-unauthenticated.exe");
            FileUtils.copyFile(sourceFile, targetFile2);
            cli.execute("--keystore=target/test-classes/keystores/" + keystore, "--alias=" + alias, "--keypass=" + keypass,
                        "--tsaurl=http://timestamp.comodoca.com/rfc3161", "--tsmode=rfc3161", "--tsretries=1", "--tsretrywait=1",
                        "--proxyUrl=localhost:" + proxy.getListenAddress().getPort(),
                        "" + targetFile2);
            
            assertTrue("The file " + targetFile2 + " wasn't changed", SOURCE_FILE_CRC32 != FileUtils.checksumCRC32(targetFile2));
            assertTrue("The proxy wasn't used", proxyUsed.get());
    
            try (PEFile peFile = new PEFile(targetFile2)) {
                List<CMSSignedData> signatures = peFile.getSignatures();
                assertNotNull(signatures);
                assertEquals(1, signatures.size());
    
                CMSSignedData signature = signatures.get(0);
    
                assertNotNull(signature);
            }
        } finally {
            proxy.stop();
        }
    }

    @Test
    public void testTimestampingWithProxyAuthenticated() throws Exception {
        final AtomicBoolean proxyUsed = new AtomicBoolean(false);
        HttpProxyServer proxy = DefaultHttpProxyServer.bootstrap().withPort(12544)
                .withFiltersSource(new HttpFiltersSourceAdapter() {
                    @Override
                    public HttpFilters filterRequest(HttpRequest originalRequest) {
                        proxyUsed.set(true);
                        return super.filterRequest(originalRequest);
                    }
                })
                .withProxyAuthenticator(new ProxyAuthenticator() {
                    @Override
                    public boolean authenticate(String username, String password) {
                        return "jsign".equals(username) && "jsign".equals(password);
                    }

                    @Override
                    public String getRealm() {
                        return "Jsign Tests";
                    }
                })
                .start();

        try {
            File targetFile2 = new File("target/test-classes/wineyes-timestamped-with-cli-rfc3161-proxy-authenticated.exe");
            FileUtils.copyFile(sourceFile, targetFile2);
            cli.execute("--keystore=target/test-classes/keystores/" + keystore, "--alias=" + alias, "--keypass=" + keypass,
                        "--tsaurl=http://timestamp.comodoca.com/rfc3161", "--tsmode=rfc3161", "--tsretries=1", "--tsretrywait=1",
                        "--proxyUrl=http://localhost:" + proxy.getListenAddress().getPort(),
                        "--proxyUser=jsign",
                        "--proxyPass=jsign",
                        "" + targetFile2);
            
            assertTrue("The file " + targetFile2 + " wasn't changed", SOURCE_FILE_CRC32 != FileUtils.checksumCRC32(targetFile2));
            assertTrue("The proxy wasn't used", proxyUsed.get());
    
            try (PEFile peFile = new PEFile(targetFile2)) {
                List<CMSSignedData> signatures = peFile.getSignatures();
                assertNotNull(signatures);
                assertEquals(1, signatures.size());
    
                CMSSignedData signature = signatures.get(0);
    
                assertNotNull(signature);
            }
        } finally {
            proxy.stop();
        }
    }

    @Test
    public void testReplaceSignature() throws Exception {
        File targetFile2 = new File("target/test-classes/wineyes-re-signed.exe");
        FileUtils.copyFile(sourceFile, targetFile2);
        cli.execute("--keystore=target/test-classes/keystores/" + keystore, "--alias=" + alias, "--keypass=" + keypass, "" + targetFile2);
        
        assertTrue("The file " + targetFile2 + " wasn't changed", SOURCE_FILE_CRC32 != FileUtils.checksumCRC32(targetFile2));
        
        cli.execute("--keystore=target/test-classes/keystores/" + keystore, "--alias=" + alias, "--keypass=" + keypass, "--alg=SHA-512", "--replace", "" + targetFile2);
        
        try (PEFile peFile = new PEFile(targetFile2)) {
            List<CMSSignedData> signatures = peFile.getSignatures();
            assertNotNull(signatures);
            assertEquals(1, signatures.size());

            assertNotNull(signatures.get(0));
            
            assertEquals("Digest algorithm", DigestAlgorithm.SHA512.oid, signatures.get(0).getDigestAlgorithmIDs().iterator().next().getAlgorithm());
        }
    }

    @Test
    public void testExitOnError() {
        NoExitSecurityManager manager = new NoExitSecurityManager();
        System.setSecurityManager(manager);

        try {
            JsignCLI.main("foo.exe");
            fail("VM not terminated");
        } catch (SecurityException e) {
            // expected
            assertEquals("Exit code", Integer.valueOf(1), manager.getStatus());
        } finally {
            System.setSecurityManager(null);
        }
    }

    private static class NoExitSecurityManager extends SecurityManager {
        private Integer status;

        public Integer getStatus() {
            return status;
        }

        public void checkPermission(Permission perm) { }
        
        public void checkPermission(Permission perm, Object context) { }

        public void checkExit(int status) {
            this.status = status;
            throw new SecurityException("Exit disabled");
        }
    }

    @Test(expected = ParseException.class)
    public void testUnknownOption() throws Exception {
        cli.execute("--jsign");
    }

    @Test
    public void testUnknownPKCS11Provider() throws Exception {
        try {
            cli.execute("--storetype=PKCS11", "--keystore=SunPKCS11-jsigntest", "--keypass=password", "" + targetFile);
            fail("No exception thrown");
        } catch (SignerException e) {
            assertEquals("exception message", "Security provider SunPKCS11-jsigntest not found", e.getMessage());
        }
    }

    @Test
    public void testMissingPKCS11Configuration() throws Exception {
        try {
            cli.execute("--storetype=PKCS11", "--keystore=jsigntest.cfg", "--keypass=password", "" + targetFile);
            fail("No exception thrown");
        } catch (SignerException e) {
            assertEquals("keystore option should either refer to the SunPKCS11 configuration file or to the name of the provider configured in jre/lib/security/java.security", e.getMessage());
        }
    }

    @Test
    public void testBrokenPKCS11Configuration() throws Exception {
        try {
            cli.execute("--storetype=PKCS11", "--keystore=pom.xml", "--keypass=password", "" + targetFile);
            fail("No exception thrown");
        } catch (SignerException e) {
            // expected
            assertTrue(e.getCause().getCause() instanceof ProviderException // JDK < 9
                    || e.getCause().getCause() instanceof InvalidParameterException); // JDK 9+
        }
    }
}
