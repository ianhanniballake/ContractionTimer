/* Copyright (c) 2012 Google Inc.
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
package com.ianhanniballake.contractiontimer.inappbilling;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents an in-app product's listing details.
 */
@SuppressWarnings("javadoc")
public class SkuDetails
{
	String mDescription;
	String mJson;
	String mPrice;
	String mSku;
	String mTitle;
	String mType;

	public SkuDetails(final String jsonSkuDetails) throws JSONException
	{
		mJson = jsonSkuDetails;
		final JSONObject o = new JSONObject(mJson);
		mSku = o.optString("productId");
		mType = o.optString("type");
		mPrice = o.optString("price");
		mTitle = o.optString("title");
		mDescription = o.optString("description");
	}

	public String getDescription()
	{
		return mDescription;
	}

	public String getPrice()
	{
		return mPrice;
	}

	public String getSku()
	{
		return mSku;
	}

	public String getTitle()
	{
		return mTitle;
	}

	public String getType()
	{
		return mType;
	}

	@Override
	public String toString()
	{
		return "SkuDetails:" + mJson;
	}
}
