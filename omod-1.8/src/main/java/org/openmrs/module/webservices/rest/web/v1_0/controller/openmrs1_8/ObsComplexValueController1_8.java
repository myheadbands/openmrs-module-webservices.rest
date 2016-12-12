/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.webservices.rest.web.v1_0.controller.openmrs1_8;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.ConceptComplex;
import org.openmrs.Obs;
import org.openmrs.api.ConceptService;
import org.openmrs.api.ObsService;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.response.IllegalRequestException;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.openmrs.obs.ComplexData;
import org.openmrs.obs.ComplexObsHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;

@Controller
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/obs")
public class ObsComplexValueController1_8 extends BaseRestController {

	@Autowired
	ObsService obsService;

	@Autowired
	ConceptService conceptService;

	@RequestMapping(value = "/{uuid}/value", method = RequestMethod.GET)
	public void getFile(@PathVariable("uuid") String uuid, HttpServletResponse response) throws Exception {
		Obs obs = obsService.getObsByUuid(uuid);
		if (!obs.isComplex()) {
			throw new IllegalRequestException("It is not a complex obs, thus have no data.");
		}

		ConceptComplex concept = conceptService.getConceptComplex(obs.getConcept().getConceptId());
		ComplexObsHandler handler = obsService.getHandler(concept.getHandler());

		obs = handler.getObs(obs, "RAW_VIEW");
		ComplexData complexData = obs.getComplexData();

		String mimeType;
		try {
			mimeType = BeanUtils.getProperty(complexData, "mimeType");
		} catch (Exception e) {
			mimeType = "application/force-download"; //no mimeType for openmrs-api 1.11 and below
		}

		response.setContentType(mimeType);
		if (StringUtils.isNotBlank(complexData.getTitle())) {
			response.setHeader("Content-Disposition", "attachment; filename=" + complexData.getTitle());
		}
		Object data = complexData.getData();
		if (data instanceof byte[]) {
			response.getOutputStream().write((byte[]) data);
		} else if (data instanceof InputStream) {
			IOUtils.copy((InputStream) data, response.getOutputStream());
		}

		response.flushBuffer();
	}
}
