#!/usr/bin/env python

import ConfigParser
import StringIO
import email.mime.text
import os
import os.path
import smtplib

import lxml.etree

config = ConfigParser.ConfigParser()
config.read(['faust.ini', "local.ini"])

xml_dir = config.get("xml", "dir")
report_sender = "Faust-Edition <noreply@faustedition.net>"
report_recipients = ["Gregor Middell <gregor@middell.net>"]

def relative_path(xml_file):
	"""Returns the path of the given XML file relative to the base directory"""
	prefix = xml_dir + "/"
	if xml_file.startswith(prefix):
		return xml_file[len(prefix):]
	else:
		return xml_file

def absolute_path(xml_file):
	"""Returns the absolute path of the XML file specified relative to the base directory"""
	return "/".join((xml_dir, xml_file))
		 
def xml_files():
	"""Returns paths of all XML documents in the edition"""
	xml_files = []
	for root, dirs, files in os.walk(xml_dir):		
		for f in files: 
			if f.endswith(".xml"): xml_files.append(os.path.join(root, f))
	xml_files.sort()
	return xml_files

def is_tei_document(xml_file):
	"""Determines whether a XML file is a TEI document by checking the namespace of the first element encountered"""
	for event, element in lxml.etree.iterparse(xml_file):
		if element is None: continue
		return element.tag.startswith("{http://www.tei-c.org/ns/1.0}")

def send_report(subject, msg):
	if config.getboolean("mail", "enabled"):
		msg = email.mime.text.MIMEText(msg)
		msg["Subject"] = subject
		msg["From"] = report_sender
		msg["To"] = ", ".join(report_recipients)
		
		server = smtplib.SMTP('localhost')
		server.sendmail(report_sender, report_recipients, msg.as_string())
		server.quit()
	else:
		print "Subject:", subject
		print
		print msg
	

tei_serialization_xslt = StringIO.StringIO('''\
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output indent="no" omit-xml-declaration="yes" />
	<!-- <xsl:strip-space elements="*" /> -->
	<xsl:template match="/">
		<xsl:processing-instruction name="oxygen">RNGSchema="%s" type="xml"</xsl:processing-instruction>
		<xsl:apply-templates select="*[position() = last()]"/>
	</xsl:template>
	<xsl:template match="@*|node()">
		<xsl:copy>
			<xsl:apply-templates select="@*|node()" />
		</xsl:copy>
	</xsl:template>
</xsl:stylesheet>''' % config.get("validate", "schema"))
tei_serialize = lxml.etree.XSLT(lxml.etree.parse(tei_serialization_xslt))
	
xpath_namespaces = {
	"tei": "http://www.tei-c.org/ns/1.0",
        "ge": "http://www.tei-c.org/ns/geneticEditions",
        "f": "http://www.faustedition.net/ns",
        "svg": "http://www.w3.org/2000/svg",
        "exist": "http://exist.sourceforge.net/NS/exist"
}
def xpath(expr):
	return lxml.etree.XPath(expr, namespaces=xpath_namespaces)