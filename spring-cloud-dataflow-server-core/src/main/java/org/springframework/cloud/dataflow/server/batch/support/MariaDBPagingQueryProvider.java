/*
 * Copyright 2006-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.dataflow.server.batch.support;

import org.springframework.cloud.dataflow.server.batch.DataflowSqlPagingQueryProvider;
import org.springframework.util.StringUtils;

/**
 * MariaDB implementation of a {@link DataflowSqlPagingQueryProvider} using database specific features.
 *
 * @author Thomas Risberg
 * @author Michael Minella
 * @author Corneil du Plessis
 */
public class MariaDBPagingQueryProvider extends AbstractSqlPagingQueryProvider {

	@Override
	public String generateFirstPageQuery(int pageSize) {
		return SqlPagingQueryUtils.generateLimitSqlQuery(this, false, buildLimitClause(pageSize));
	}

	@Override
	public String generateRemainingPagesQuery(int pageSize) {
		if(StringUtils.hasText(getGroupClause())) {
			return SqlPagingQueryUtils.generateLimitGroupedSqlQuery(this, true, buildLimitClause(pageSize));
		}
		else {
			return SqlPagingQueryUtils.generateLimitSqlQuery(this, true, buildLimitClause(pageSize));
		}
	}

	private String buildLimitClause(int pageSize) {
		return "LIMIT " + pageSize;
	}

	@Override
	public String generateJumpToItemQuery(int itemIndex, int pageSize) {
		int page = itemIndex / pageSize;
		int offset = Math.max((page * pageSize) - 1, 0);

		return SqlPagingQueryUtils.generateLimitJumpToQuery(this, "LIMIT " + offset + ", 1");
	}

}
