/**
 * Copyright 2010-2015 Atilika Inc. and contributors (see CONTRIBUTORS.md)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.  A copy of the
 * License is distributed with this work in the LICENSE.md file.  You may
 * also obtain a copy of the License from
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.atilika.kuromoji;

import com.atilika.kuromoji.AbstractToken;
import com.atilika.kuromoji.AbstractTokenizer;

import java.util.ArrayList;
import java.util.List;

/**
 * A tokenizer that works across all kuromoji dictionaries
 */
public class Tokenizer {

    private Dictionary dictionary = null;
    private AbstractTokenizer tokenizer = null;

    public Tokenizer(Dictionary dictionary) {
        this.dictionary = dictionary;
    }

    public Tokenizer(String dictionaryName) throws Dictionary.InvalidDictionaryException {
        this(Dictionary.getDictionary(dictionaryName));
    }

    public Tokenizer() throws Dictionary.InvalidDictionaryException {
        this(Dictionary.getDictionary(null));
    }

    public List<Token> tokenize(String text) {
        if (tokenizer == null) {
            tokenizer = dictionary.getTokenizer();
        }
        List<Token> tokens = new ArrayList<Token>();
        List<AbstractToken> abstractTokens = tokenizer.tokenize(text);
        for (AbstractToken token : abstractTokens) {
            tokens.add(new Token(dictionary, token));
        }
        return tokens;
    }
}
