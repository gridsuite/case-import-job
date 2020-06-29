<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:iidm="http://www.powsybl.org/schema/iidm/1_2"> 
  <xsl:output method="xml" indent="yes"/>
  <xsl:param name="dateReplacement"/>

  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="iidm:network/@caseDate">
    <xsl:attribute name="caseDate">
      <xsl:value-of select="$dateReplacement"/>
    </xsl:attribute>
  </xsl:template>

</xsl:stylesheet>