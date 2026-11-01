package com.farmvision.agrirover.device

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class NetworkSecurityConfigXmlTest {

    @Test
    fun networkSecurityConfigXml_enforcesStrictCleartextWhitelist() {
        val xmlFile = File("src/main/res/xml/network_security_config.xml")
        val candidateFiles = listOf(
            xmlFile,
            File("app/src/main/res/xml/network_security_config.xml")
        )
        val file = candidateFiles.firstOrNull { it.exists() }
        assertNotNull("network_security_config.xml must exist in the project", file)

        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(file)
        doc.documentElement.normalize()

        assertEquals("network-security-config", doc.documentElement.tagName)

        // 1. Verify base-config blocks cleartext
        val baseConfigList = doc.getElementsByTagName("base-config")
        assertTrue("base-config must be defined", baseConfigList.length >= 1)
        val baseConfig = baseConfigList.item(0) as Element
        assertEquals("false", baseConfig.getAttribute("cleartextTrafficPermitted"))

        // 2. Verify domain-config permits only approved local hosts
        val domainConfigList = doc.getElementsByTagName("domain-config")
        assertTrue("domain-config must be defined", domainConfigList.length >= 1)
        val domainConfig = domainConfigList.item(0) as Element
        assertEquals("true", domainConfig.getAttribute("cleartextTrafficPermitted"))

        val domainNodes = domainConfig.getElementsByTagName("domain")
        val approvedDomains = setOf("192.168.4.1", "192.168.4.2", "192.168.4.0", "10.0.2.2", "127.0.0.1", "localhost")
        val foundDomains = mutableSetOf<String>()

        for (i in 0 until domainNodes.length) {
            val domainElem = domainNodes.item(i) as Element
            val host = domainElem.textContent.trim()
            foundDomains.add(host)
            assertTrue("Domain '$host' is not in the approved local cleartext whitelist", approvedDomains.contains(host))
            assertEquals("includeSubdomains must be false for IP literal '$host'", "false", domainElem.getAttribute("includeSubdomains"))
        }

        assertEquals("All approved local domains must be declared", approvedDomains, foundDomains)
    }
}
