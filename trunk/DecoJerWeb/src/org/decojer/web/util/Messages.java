/*
 * $Id$
 *
 * This file is part of the DecoJer project.
 * Copyright (C) 2010-2011  André Pankraz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * In accordance with Section 7(b) of the GNU Affero General Public License,
 * a covered work must retain the producer line in every Java Source Code
 * that is created using DecoJer.
 */
package org.decojer.web.util;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * 
 * @author André Pankraz
 */
public class Messages {

	public static void addMessage(final HttpServletRequest req,
			final String message) {
		List<String> messages = getMessages(req.getSession());
		if (messages == null) {
			messages = new ArrayList<String>();
		}
		messages.add(message);
		req.getSession().setAttribute("messages", messages); // trigger update
	}

	public static List<String> getMessages(final HttpSession httpSession) {
		return (List<String>) httpSession.getAttribute("messages");
	}

	public static String getMessagesHtml(final HttpSession httpSession) {
		final List<String> messages = getMessages(httpSession);
		if (messages == null || messages.size() == 0) {
			return "";
		}
		final StringBuilder sb = new StringBuilder("<ul>");
		for (final String message : messages) {
			sb.append("<li>").append(message).append("</li>");
		}
		httpSession.removeAttribute("messages");
		sb.append("</ul>");
		return sb.toString();
	}

}