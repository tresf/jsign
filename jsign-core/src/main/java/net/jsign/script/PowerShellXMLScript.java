/**
 * Copyright 2019 Emmanuel Bourg
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

package net.jsign.script;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.bouncycastle.asn1.ASN1Object;

import net.jsign.asn1.authenticode.SpcSipInfo;
import net.jsign.asn1.authenticode.SpcUuid;

/**
 * A PowerShell script in XML format (.ps1xml).
 *
 * @author Emmanuel Bourg
 * @since 3.0
 */
public class PowerShellXMLScript extends SignableScript {

    /**
     * Create a PowerShell script.
     * The encoding is assumed to be UTF-8.
     */
    public PowerShellXMLScript() {
        super();
    }

    /**
     * Create a PowerShell script from the specified file and load its content.
     * The encoding is assumed to be UTF-8.
     * 
     * @param file the PowerShell script
     * @throws IOException if an I/O error occurs
     */
    public PowerShellXMLScript(File file) throws IOException {
        super(file, StandardCharsets.UTF_8);
    }

    /**
     * Create a PowerShell script from the specified file and load its content.
     * 
     * @param file     the PowerShell script
     * @param encoding the encoding of the script (if null the default UTF-8 encoding is used)
     * @throws IOException if an I/O error occurs
     */
    public PowerShellXMLScript(File file, Charset encoding) throws IOException {
        super(file, encoding);
    }

    @Override
    String getSignatureStart() {
        return "<!-- SIG # Begin signature block -->";
    }

    @Override
    String getSignatureEnd() {
        return "<!-- SIG # End signature block -->";
    }

    @Override
    String getLineCommentStart() {
        return "<!-- ";
    }

    @Override
    String getLineCommentEnd() {
        return " -->";
    }

    @Override
    ASN1Object getSpcSipInfo() {
        return new SpcSipInfo(65536, new SpcUuid("1FCC3B60-594B-084E-B724-D2C6297EF351"));
    }
}
