/*
 * Copyright 2013 the original author or authors.
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

package ratpack.groovy

import ratpack.groovy.guice.GroovyModuleRegistry
import ratpack.groovy.handling.GroovyChain
import ratpack.groovy.internal.RatpackScriptBacking
import ratpack.groovy.launch.GroovyScriptFileHandlerFactory
import ratpack.groovy.templating.EphemeralPortScriptBacking
import ratpack.launch.LaunchConfig
import ratpack.launch.LaunchConfigBuilder
import ratpack.server.RatpackServer
import ratpack.server.internal.ServiceBackedServer
import ratpack.test.internal.RatpackGroovyScriptAppSpec

class StandaloneScriptSpec extends RatpackGroovyScriptAppSpec {

  @Override
  File getRatpackFile() {
    file("custom.groovy")
  }

  @Override
  protected LaunchConfig createLaunchConfig() {
    LaunchConfigBuilder.baseDir(ratpackFile.parentFile).build(new GroovyScriptFileHandlerFactory())
  }

  @Override
  RatpackServer createServer(LaunchConfig launchConfig) {
    def service = new ScriptBackedService({
      def shell = new GroovyShell(getClass().classLoader)
      def script = shell.parse(ratpackFile)
      Thread.start {
        RatpackScriptBacking.withBacking(new EphemeralPortScriptBacking()) {
          script.run()
        }
      }
    })
    new ServiceBackedServer(service, launchConfig)
  }

  def "can execute plain script and reload"() {
    when:
    app {
      script """
        ratpack {
          handlers {
            get {
              response.send "foo"
            }
          }
        }
      """
    }

    then:
    getText() == "foo"

    when:
    script """
      ratpack {
        handlers {
          get {
            response.send "bar"
          }
        }
      }
    """

    then:
    getText() == "bar"
  }

  def "types in API are correct"() {
    when:
    script """
      ratpack {
        modules {
          assert delegate instanceof $GroovyModuleRegistry.name
        }
        handlers {
          assert delegate instanceof $GroovyChain.name
          get {
            render "ok"
          }
        }
      }
    """

    then:
    text == "ok"
  }
}
