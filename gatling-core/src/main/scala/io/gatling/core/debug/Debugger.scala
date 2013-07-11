/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.core.debug

import scala.collection.mutable
import scala.concurrent.duration.DurationInt

import akka.actor.Props
import io.gatling.core.action.{ BaseActor, system }
import io.gatling.core.config.GatlingConfiguration.configuration

object Debugger {

	import io.gatling.core.action.system.dispatcher

	val flushPeriodInMs = 5000
	val debugger = system.actorOf(Props(new Debugger))

	system.scheduler.schedule(0 millisecond, Debugger.flushPeriodInMs milliseconds, debugger, DebugFlush)
}

class Debugger extends BaseActor {

	private var lastTouch: Long = _
	private def touch { lastTouch = System.nanoTime }
	touch
	private val events = mutable.Map.empty[Int, String]

	override def preStart {
		context.setReceiveTimeout(configuration.core.timeOut.simulation seconds)
	}

	def receive = {
		case DebugEvent(userId, message) =>
			touch
			events += userId -> message
		case DebugEnd(userId) =>
			touch
			events -= userId
		case DebugFlush =>
			val timeSinceLastTouch = (System.nanoTime - lastTouch) / 1000000 // in ms
			if (timeSinceLastTouch > Debugger.flushPeriodInMs) {
				System.err.println(s"No new event for $timeSinceLastTouch, alive users last event")
				System.err.println(events.mkString("\n"))
			}
	}
}
