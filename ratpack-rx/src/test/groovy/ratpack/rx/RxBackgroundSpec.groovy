/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ratpack.rx

import ratpack.error.ServerErrorHandler
import ratpack.error.internal.PrintingServerErrorHandler
import ratpack.test.internal.RatpackGroovyDslSpec

import static ratpack.rx.Rx.rxBackground

class RxBackgroundSpec extends RatpackGroovyDslSpec {

  def setup() {
    modules.add(new RxModule())
  }

  def "can observe the background"() {
    when:
    handlers {
      get(":value") {
        rxBackground {
          pathTokens.value
        } map {
          it * 2
        } map {
          it.toUpperCase()
        } subscribe {
          render it
        }
      }
    }

    then:
    getText("a") == "AA"
  }

  def "background errors are sent to the context renderer"() {
    when:
    modules {
      bind ServerErrorHandler, new PrintingServerErrorHandler()
    }
    handlers {
      get(":value") {
        rxBackground {
          pathTokens.value
        } map {
          it * 2
        } map {
          throw new Exception("!!!!")
        } subscribe {
          render "shouldn't happen"
        }
      }
    }

    then:
    getText("a").contains new Exception("!!!!").message
    response.statusCode == 500
  }
}
