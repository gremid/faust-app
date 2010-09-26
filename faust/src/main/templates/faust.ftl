<#assign cp = config["ctx.path"]>

<#macro page title css="" header="">
<!DOCTYPE html>
<html>
<head>
	<title>Digitale Faust-Edition: ${title}</title>
	<link rel="stylesheet" type="text/css" href="${cp}/static/yui3/build/cssreset/reset-min.css">
	<link rel="stylesheet" type="text/css" href="${cp}/static/yui3/build/cssfonts/fonts-min.css">
	<link rel="stylesheet" type="text/css" href="${cp}/static/yui3/build/cssgrids/grids-min.css">
	<link rel="stylesheet" type="text/css" href="${cp}/static/yui3/build/cssbase/base-min.css"> 
	<link rel="stylesheet" type="text/css" href="${cp}/static/css/faust.css">
	<script type="text/javascript">
		var cp = "${cp}"; 
		var iip = "${config['facsimile.iip.url']}"; 
		document.documentElement.className = "yui3-loading";
	</script>
	<script type="text/javascript" src="${cp}/static/yui3/build/yui/yui-min.js"></script>
	<script type="text/javascript" src="${cp}/static/js/faust.js"></script>
	<link rel="schema.DC" href="http://purl.org/dc/elements/1.1/">
	<link rel="schema.DCTERMS" href="http://purl.org/dc/terms/">
	<meta name="DC.format" scheme="DCTERMS.IMT" content="text/html">
	<meta name="DC.type" scheme="DCTERMS.DCMIType" content="Text">
	<meta name="DC.publisher" content="Digitale Faust-Edition">
	<meta name="DC.creator" content="Digitale Faust-Edition">
	<meta name="DC.subject" content="Faust, Johann Wolfgang von Goethe, Historisch-kritische Edition, digital humanities">
	<!-- 
	<meta name="DCTERMS.license"  scheme="DCTERMS.URI" content="http://www.gnu.org/copyleft/fdl.html">
	<meta name="DCTERMS.rightsHolder" content="Wikimedia Foundation Inc.">
	 -->
	 <#if css?has_content><style type="text/css">${css}</style></#if>
	 <#if header?has_content>${header}</#if>
</head>
<body class="yui3-skin-sam">
<div id="header">
	<div class="yui3-g">
		<div class="yui3-u-2-3"><h1><span class="color-1">Digitale Faustedition:&#160;</span>${title}</h1></div>
		<div class="yui3-u-1-3">
			<form id="search-form" action="${cp}/search" method="get">
				<input id="term" type="text" value="" disabled="disabled"/>
				<input type="submit" value="${message('search')}" disabled="disabled"/>
			</form>
		</div>		
	</div>
	<@topNavigation />
</div>
<div id="main">
	<#nested />
</div>
<div id="footer">
	<p>Digitale Faust-Edition. Copyright (c) 2009, 2010 Freies Deutsches Hochstift Frankfurt, Klassik Stiftung Weimar, Universität Würzburg.</p>
</div>
<script type="text/javascript">
	Faust.YUI().use("node", "dom", "node-menunav", function(Y) { 
		topNav = Y.one("#top-navigation")
		topNav.plug(Y.Plugin.NodeMenuNav); 
		topNav.get("ownerDocument").get("documentElement").removeClass("yui3-loading");
		
		//Y.one("#search-form #term").focus();
	});
</script>
</body>
</html>
</#macro>

<#macro topNavigation>
<div id="top-navigation" class="yui3-menu yui3-menu-horizontal yui3-menubuttonnav">
	<div class="yui3-menu-content">
		<ul class="first-of-type">
			<#if roles?seq_contains("editor")>							
			<li>
				<a href="#documents" class="yui3-menu-label"><em>${message("menu.document")}</em></a>
				<div id="documents" class="yui3-menu">
				<div class="yui3-menu-content">
				<ul>
					<li class="yui3-menuitem"><a href="${cp}/archive/" class="yui3-menuitem-content">${message("menu.archives")}</a></li>
					<li class="yui3-menuitem"><a href="${cp}/document/faust/2/gsa_391098.xml" class="yui3-menuitem-content"><em>GSA/ W 1804 (XIX,3) – H</em></a></li>
					<li class="yui3-menuitem"><a href="${cp}/document/faust/2.5/gsa_390883.xml" class="yui3-menuitem-content"><em>GSA/ W 1698 (XVIII,7,3) – V H.2</em></a></li>
				</ul>
				</div>
				</div>
			</li>
			<li class="yui3-menuitem"><a href="${cp}/text/sample" class="yui3-menuitem-content">${message("menu.text")}</a></li>
			<li class="yui3-menuitem"><a href="${cp}/genesis/" class="yui3-menuitem-content">${message("menu.genesis")}</a></li>
			</#if>
			<li><a class="yui3-menu-label" href="${cp}/project/about"><em>${message("menu.project")}</em></a>
				<div id="project" class="yui3-menu">
				<div class="yui3-menu-content">
				<ul>
					<li class="yui3-menuitem"><a href="${cp}/static/dfg-grant-application.pdf" class="yui3-menuitem-content">${message("menu.grant_application")}</a></li>
				</ul>
				</div>
				</div>
			</li>
			<li><a class="yui3-menu-label" href="#partners"><em>${message("menu.partners")}</em></a>
				<div id="partners" class="yui3-menu">
				<div class="yui3-menu-content">
				<ul>
					<li class="yui3-menuitem"><a href="http://www.goethehaus-frankfurt.de/" class="yui3-menuitem-content">Freies Deutsches Hochstift Frankfurt</a></li>
					<li class="yui3-menuitem"><a href="http://www.klassik-stiftung.de/" class="yui3-menuitem-content">Klassik Stiftung Weimar</a></li>
					<li class="yui3-menuitem"><a href="http://www.uni-wuerzburg.de/" class="yui3-menuitem-content">Julius-Maximilians-Universität Würzburg</a></li>
				</ul>
				</div>
				</div>
			</li>
			<li class="yui3-menuitem"><a href="${cp}/project/contact" class="yui3-menuitem-content">${message("menu.contact")}</a></li>
			<li class="yui3-menuitem"><a href="${cp}/project/imprint" class="yui3-menuitem-content">${message("menu.imprint")}</a></li>
			<li class="yui3-menuitem"><a href="https://faustedition.uni-wuerzburg.de/" class="yui3-menuitem-content">${message("menu.restricted")}</a></li>
			<#if !roles?seq_contains("editor")>
			<li class="yui3-menuitem"><a href="${cp}/login" class="yui3-menuitem-content">Login</a></li>
			</#if>
		</ul>
	</div>
</div>	
</#macro>