/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.wicket.markup;

import java.util.ArrayList;

import org.apache.wicket.Page;
import org.apache.wicket.markup.parser.IMarkupFilter;
import org.apache.wicket.markup.parser.IXmlPullParser;
import org.apache.wicket.markup.parser.filter.EnclosureHandler;
import org.apache.wicket.markup.parser.filter.HeadForceTagIdHandler;
import org.apache.wicket.markup.parser.filter.HtmlHandler;
import org.apache.wicket.markup.parser.filter.HtmlHeaderSectionHandler;
import org.apache.wicket.markup.parser.filter.OpenCloseTagExpander;
import org.apache.wicket.markup.parser.filter.RelativePathPrefixHandler;
import org.apache.wicket.markup.parser.filter.WicketLinkTagHandler;
import org.apache.wicket.markup.parser.filter.WicketMessageTagHandler;
import org.apache.wicket.markup.parser.filter.WicketNamespaceHandler;
import org.apache.wicket.markup.parser.filter.WicketRemoveTagHandler;
import org.apache.wicket.markup.parser.filter.WicketTagIdentifier;

/**
 * This is Wicket's default markup parser. It gets pre-configured with Wicket's default wicket
 * filters.
 * 
 * @see MarkupFactory
 * 
 * @author Juergen Donnerstag
 */
public class MarkupParser extends AbstractMarkupParser
{
	/**
	 * Constructor.
	 * 
	 * @param resource
	 *            The markup resource (file)
	 */
	public MarkupParser(final MarkupResourceStream resource)
	{
		super(resource);
	}

	/**
	 * Constructor. Usually for testing purposes only
	 * 
	 * @param markup
	 *            The markup resource.
	 */
	public MarkupParser(final String markup)
	{
		super(markup);
	}

	/**
	 * Constructor.
	 * 
	 * @param xmlParser
	 *            The streaming xml parser to read and parse the markup
	 * @param resource
	 *            The markup resource (file)
	 */
	public MarkupParser(final IXmlPullParser xmlParser, final MarkupResourceStream resource)
	{
		super(xmlParser, resource);
	}

	/**
	 * @see org.apache.wicket.markup.MarkupParser#getMarkupFilters()
	 */
	@Override
	public MarkupFilterList getMarkupFilters()
	{
		return (MarkupFilterList)super.getMarkupFilters();
	}

	/**
	 * Add a markup filter
	 * 
	 * @param filter
	 * @return true, if successful
	 */
	public final boolean add(final IMarkupFilter filter)
	{
		return getMarkupFilters().add(filter);
	}

	/**
	 * Add a markup filter before the 'beforeFilter'
	 * 
	 * @param filter
	 * @param beforeFilter
	 * @return true, if successful
	 */
	public final boolean add(final IMarkupFilter filter,
		final Class<? extends IMarkupFilter> beforeFilter)
	{
		return getMarkupFilters().add(filter, beforeFilter);
	}

	/**
	 * a) Allow subclasses to configure individual Wicket filters
	 * <p>
	 * b) Allows to disable Wicket filters via returning false
	 * 
	 * @param filter
	 * @return If false, the filter will not be added
	 */
	protected boolean onAppendMarkupFilter(final IMarkupFilter filter)
	{
		return true;
	}

	/**
	 * Initialize Wicket's MarkupParser with all necessary markup filters. You may subclass this
	 * method, to add your own filters to the list.
	 * 
	 * @param markup
	 * @return The list of markup filter
	 */
	@Override
	protected MarkupFilterList initializeMarkupFilters(final Markup markup)
	{
		// MarkupFilterList is a simple extension of ArrayList providing few additional helpers
		final MarkupFilterList filters = new MarkupFilterList()
		{
			private static final long serialVersionUID = 1L;

			/**
			 * @see org.apache.wicket.markup.MarkupFactory.MarkupFilterList#onAdd(org.apache.wicket.markup.parser.IMarkupFilter)
			 */
			@Override
			protected boolean onAdd(final IMarkupFilter filter)
			{
				// a) allow users to configure wicket filters
				// b) if return value == false, the filter will not be added
				return onAppendMarkupFilter(filter);
			}
		};

		MarkupResourceStream markupResourceStream = markup.getMarkupResourceStream();

		filters.add(new WicketTagIdentifier(markupResourceStream));
		filters.add(new HtmlHandler());
		filters.add(new WicketRemoveTagHandler());
		filters.add(new WicketLinkTagHandler());
		filters.add(new WicketNamespaceHandler(markupResourceStream));

		// Provided the wicket component requesting the markup is known ...
		if ((markupResourceStream != null) && (markupResourceStream.getResource() != null))
		{
			final ContainerInfo containerInfo = markupResourceStream.getContainerInfo();
			if (containerInfo != null)
			{
				filters.add(new WicketMessageTagHandler());

				// Pages require additional handlers
				if (Page.class.isAssignableFrom(containerInfo.getContainerClass()))
				{
					filters.add(new HtmlHeaderSectionHandler(markup));
				}

				filters.add(new HeadForceTagIdHandler(containerInfo.getContainerClass()));
			}
		}

		filters.add(new OpenCloseTagExpander());
		filters.add(new RelativePathPrefixHandler());
		filters.add(new EnclosureHandler());

		return filters;
	}

	/**
	 * A simple extension to ArrayList to manage Wicket MarkupFilter's more easily
	 */
	public static class MarkupFilterList extends ArrayList<IMarkupFilter>
	{
		private static final long serialVersionUID = 1L;

		/**
		 * @see java.util.ArrayList#add(java.lang.Object)
		 */
		@Override
		public boolean add(final IMarkupFilter filter)
		{
			return add(filter, RelativePathPrefixHandler.class);
		}

		/**
		 * Insert a markup filter before a another one.
		 * 
		 * @param filter
		 * @param beforeFilter
		 * @return true, if successful
		 */
		public boolean add(final IMarkupFilter filter,
			final Class<? extends IMarkupFilter> beforeFilter)
		{
			if (onAdd(filter) == false)
			{
				return false;
			}

			int index = indexOf(beforeFilter);
			if (index < 0)
			{
				return super.add(filter);
			}

			super.add(index, filter);
			return true;
		}

		/**
		 * a) Allow subclasses to configure individual Wicket filters which otherwise can not be
		 * accessed.
		 * <p>
		 * b) Allows to disable Wicket filters via returning false
		 * 
		 * @param filter
		 * @return If false, the filter will not be added
		 */
		protected boolean onAdd(final IMarkupFilter filter)
		{
			return true;
		}
	}
}
