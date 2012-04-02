package hector.js

/**
 * The JsTopLevel object mirrors the JavaScript top-level scope.
 *
 * <p>All top-level elements are prefixed with "js" so they do not clash with definitions in
 * the code or Scala automated imports like <code>String</code>.</p>
 *
 * <p>The only member for all JavaScript objects which has been redefined is toString and is named
 * <code>jsToString</code> so it is clear whether or not you intend to generate an expression.</p>
 */
object toplevel {
  // Top-Level Constants

  def jsInfinity = new JsIdentifier('Infinity) with JsNumberType

  def jsNaN = new JsIdentifier('NaN) with JsNumberType

  def jsUndefined = JsIdentifier('undefined)

  // Top-Level Functions

  def jsDecodeURI = new JsIdentifier('decodeURI) with JsStringType

  def jsDecodeURIComponent = new JsIdentifier('decodeURIComponent) with JsStringType

  def jsEncodeURI = new JsIdentifier('encodeURI) with JsStringType

  def jsEncodeURIComponent = new JsIdentifier('encodeURI) with JsStringType

  def jsEscape = new JsIdentifier('escape) with JsStringType

  def jsEval = new JsIdentifier('eval) with JsObjectType

  def jsIsFinite = new JsIdentifier('isFinite) with JsBooleanType

  def jsIsNaN = new JsIdentifier('isNaN) with JsBooleanType

  def jsParseFloat = new JsIdentifier('parseFloat) with JsNumberType

  def jsParseInt = new JsIdentifier('parseInt) with JsNumberType

  def jsUnescape = new JsIdentifier('unescape) with JsStringType

  // Top-Level Objects / Functions

  def jsArray = new JsIdentifier('Array) with JsArrayType

  def jsBoolean = new JsIdentifier('Boolean) with JsBooleanType

  def jsDate = new JsIdentifier('Date) with JsDateType

  def jsMath = new JsIdentifier('Math) with JsMathType

  def jsNumber = new JsIdentifier('Number) with JsNumberType

  def jsString = new JsIdentifier('String) with JsStringType

  def jsWindow = new JsIdentifier('window) with JsDOMWindowType

  trait JsObjectType {
    self: JsExpression ⇒

    //TODO this should be part of JsExpression!

    def constructor = new JsMember(this, JsIdentifier('constructor)) with JsObjectType

    def jsToString = new JsMember(this, JsIdentifier('toString)) with JsStringType

    def valueOf = new JsMember(this, JsIdentifier('valueOf)) with JsObjectType

    def prototype = new JsMember(this, JsIdentifier('prototype)) with JsObjectType

    def asArray = new JsNop(this) with JsArrayType

    def asBoolean = new JsNop(this) with JsBooleanType

    def asDate = new JsNop(this) with JsDateType

    def asNumber = new JsNop(this) with JsNumberType

    def asString = new JsNop(this) with JsStringType

    def asRegExp = new JsNop(this) with JsRegExpType
  }

  trait JsArrayType extends JsObjectType {
    self: JsExpression ⇒

    def length = new JsMember(this, JsIdentifier('length)) with JsNumberType

    def concat = new JsMember(this, JsIdentifier('concat)) with JsArrayType

    def indexOf = new JsMember(this, JsIdentifier('indexOf)) with JsNumberType

    def join = new JsMember(this, JsIdentifier('join)) with JsStringType

    def lastIndexOf = new JsMember(this, JsIdentifier('lastIndexOf)) with JsNumberType

    def pop = new JsMember(this, JsIdentifier('pop)) with JsObjectType

    def push = new JsMember(this, JsIdentifier('push)) with JsNumberType

    def reverse = new JsMember(this, JsIdentifier('reverse)) with JsArrayType

    def shift = new JsMember(this, JsIdentifier('shift)) with JsObjectType

    def slice = new JsMember(this, JsIdentifier('slice)) with JsArrayType

    def sort = new JsMember(this, JsIdentifier('sort)) with JsArrayType

    def splice = new JsMember(this, JsIdentifier('splice)) with JsArrayType

    def unshift = new JsMember(this, JsIdentifier('unshift)) with JsNumberType

    //TODO(joa): what about apply and update?
  }

  trait JsBooleanType extends JsObjectType {
    self: JsExpression ⇒
  }

  trait JsDateType extends JsObjectType {
    self: JsExpression ⇒

    def getDate = new JsMember(this, JsIdentifier('getDate)) with JsNumberType

    def getDay = new JsMember(this, JsIdentifier('getDay)) with JsNumberType

    def getFullYear = new JsMember(this, JsIdentifier('getFullYear)) with JsNumberType

    def getHours = new JsMember(this, JsIdentifier('getHours)) with JsNumberType

    def getMilliseconds = new JsMember(this, JsIdentifier('getMilliseconds)) with JsNumberType

    def getMinutes = new JsMember(this, JsIdentifier('getMinutes)) with JsNumberType

    def getMonth = new JsMember(this, JsIdentifier('getMonth)) with JsNumberType

    def getSeconds = new JsMember(this, JsIdentifier('getSeconds)) with JsNumberType

    def getTime = new JsMember(this, JsIdentifier('getTime)) with JsNumberType

    def getTimezoneOffset = new JsMember(this, JsIdentifier('getTimezoneOffset)) with JsNumberType

    def getUTCDate = new JsMember(this, JsIdentifier('getUTCDate)) with JsNumberType

    def getUTCDay = new JsMember(this, JsIdentifier('getUTCDay)) with JsNumberType

    def getUTCFullYear = new JsMember(this, JsIdentifier('getUTCFullYear)) with JsNumberType

    def getUTCHours = new JsMember(this, JsIdentifier('getUTCHours)) with JsNumberType

    def getUTCMilliseconds = new JsMember(this, JsIdentifier('getUTCMilliseconds)) with JsNumberType

    def getUTCMinutes = new JsMember(this, JsIdentifier('getUTCMinutes)) with JsNumberType

    def getUTCMonth = new JsMember(this, JsIdentifier('getUTCMonth)) with JsNumberType

    def getUTCSeconds = new JsMember(this, JsIdentifier('getUTCSeconds)) with JsNumberType

    def getYear = new JsMember(this, JsIdentifier('getYear)) with JsNumberType

    //static!
    def parse = new JsMember(this, JsIdentifier('parse)) with JsNumberType

    def setDate = JsMember(this, JsIdentifier('setDate))

    def setFullYear = JsMember(this, JsIdentifier('setFullYear))

    def setHours = JsMember(this, JsIdentifier('setHours))

    def setMilliseconds = JsMember(this, JsIdentifier('setMilliseconds))

    def setMinutes = JsMember(this, JsIdentifier('setMinutes))

    def setMonth = JsMember(this, JsIdentifier('setMonth))

    def setSeconds = JsMember(this, JsIdentifier('setSeconds))

    def setTime = JsMember(this, JsIdentifier('setTime))

    def setUTCDate = JsMember(this, JsIdentifier('setUTCDate))

    def setUTCFullYear = JsMember(this, JsIdentifier('setUTCFullYear))

    def setUTCHours = JsMember(this, JsIdentifier('setUTCHours))

    def setUTCMilliseconds = JsMember(this, JsIdentifier('setUTCMilliseconds))

    def setUTCMinutes = JsMember(this, JsIdentifier('setUTCMinutes))

    def setUTCMonth = JsMember(this, JsIdentifier('setUTCMonth))

    def setUTCSeconds = JsMember(this, JsIdentifier('setUTCSeconds))

    @deprecated("Use the setFullYear() method instead.", "")
    def setYear = JsMember(this, JsIdentifier('setYear))

    def toDateString = new JsMember(this, JsIdentifier('toDateString)) with JsStringType

    @deprecated("Use the toGMTString() method instead.", "")
    def toGMTString = new JsMember(this, JsIdentifier('toGMTString)) with JsStringType

    def toLocaleDateString = new JsMember(this, JsIdentifier('toLocaleDateString)) with JsStringType

    def toLocaleTimeString = new JsMember(this, JsIdentifier('toLocaleTimeString)) with JsStringType

    def toLocaleString = new JsMember(this, JsIdentifier('toLocaleString)) with JsStringType

    def toTimeString = new JsMember(this, JsIdentifier('toTimeString)) with JsStringType

    def toUTCString = new JsMember(this, JsIdentifier('toUTCString)) with JsStringType

    //static!
    def UTC = new JsMember(this, JsIdentifier('UTC)) with JsNumberType
  }

  trait JsMathType extends JsObjectType {
    self: JsExpression ⇒

    //static!
    def E = new JsMember(this, JsIdentifier('E)) with JsNumberType

    //static!
    def LN2 = new JsMember(this, JsIdentifier('LN2)) with JsNumberType

    //static!
    def LN10 = new JsMember(this, JsIdentifier('LN10)) with JsNumberType

    //static!
    def LOG2E = new JsMember(this, JsIdentifier('LOG2E)) with JsNumberType

    //static!
    def LOG10E = new JsMember(this, JsIdentifier('LOG10E)) with JsNumberType

    //static!
    def PI = new JsMember(this, JsIdentifier('PI)) with JsNumberType

    def SQRT1_2 = new JsMember(this, JsIdentifier('SQRT1_2)) with JsNumberType

    def SQRT2 = new JsMember(this, JsIdentifier('SQRT2)) with JsNumberType

    def abs = new JsMember(this, JsIdentifier('abs)) with JsNumberType

    def acos = new JsMember(this, JsIdentifier('acos)) with JsNumberType

    def asin = new JsMember(this, JsIdentifier('asin)) with JsNumberType

    def atan = new JsMember(this, JsIdentifier('atan)) with JsNumberType

    def atan2 = new JsMember(this, JsIdentifier('atan2)) with JsNumberType

    def ceil = new JsMember(this, JsIdentifier('ceil)) with JsNumberType

    def cos = new JsMember(this, JsIdentifier('cos)) with JsNumberType

    def exp = new JsMember(this, JsIdentifier('exp)) with JsNumberType

    def floor = new JsMember(this, JsIdentifier('floor)) with JsNumberType

    def log = new JsMember(this, JsIdentifier('log)) with JsNumberType

    def max = new JsMember(this, JsIdentifier('max)) with JsNumberType

    def min = new JsMember(this, JsIdentifier('min)) with JsNumberType

    def pow = new JsMember(this, JsIdentifier('pow)) with JsNumberType

    def random = new JsMember(this, JsIdentifier('random)) with JsNumberType

    def round = new JsMember(this, JsIdentifier('round)) with JsNumberType

    def sin = new JsMember(this, JsIdentifier('sin)) with JsNumberType

    def sqrt = new JsMember(this, JsIdentifier('sqrt)) with JsNumberType

    def tan = new JsMember(this, JsIdentifier('tan)) with JsNumberType
  }

  trait JsNumberType extends JsObjectType {
    self: JsExpression ⇒

    //static!
    def MAX_VALUE = new JsMember(this, JsIdentifier('MAX_VALUE)) with JsNumberType

    //static!
    def MIN_VALUE = new JsMember(this, JsIdentifier('MIN_VALUE)) with JsNumberType

    //static!
    def POSITIVE_INFINITY  = new JsMember(this, JsIdentifier('POSITIVE_INFINITY)) with JsNumberType

    //static!
    def NEGATIVE_INFINITY = new JsMember(this, JsIdentifier('NEGATIVE_INFINITY)) with JsNumberType

    def toExponential = new JsMember(this, JsIdentifier('toExponential)) with JsStringType

    def toFixed = new JsMember(this, JsIdentifier('toFixed)) with JsStringType

    def toPrecision = new JsMember(this, JsIdentifier('toPrecision)) with JsStringType

  }

  trait JsStringType extends JsObjectType {
    self: JsExpression ⇒

    def length = new JsMember(this, JsIdentifier('length)) with JsNumberType

    def charAt = new JsMember(this, JsIdentifier('charAt)) with JsStringType

    def charCodeAt = new JsMember(this, JsIdentifier('charCodeAt)) with JsNumberType

    def concat = new JsMember(this, JsIdentifier('concat)) with JsStringType

    //static!
    def fromCharCode = new JsMember(this, JsIdentifier('fromCharCode)) with JsStringType

    def indexOf = new JsMember(this, JsIdentifier('indexOf)) with JsNumberType

    def lastIndexOf = new JsMember(this, JsIdentifier('lastIndexOf)) with JsNumberType

    def `match` = new JsMember(this, JsIdentifier('match)) with JsArrayType

    def replace = new JsMember(this, JsIdentifier('replace)) with JsStringType

    def search = new JsMember(this, JsIdentifier('search)) with JsNumberType

    def slice = new JsMember(this, JsIdentifier('slice)) with JsStringType

    def split = new JsMember(this, JsIdentifier('split)) with JsArrayType

    def substr = new JsMember(this, JsIdentifier('substr)) with JsStringType

    def substring = new JsMember(this, JsIdentifier('substring)) with JsStringType

    def toLowerCase = new JsMember(this, JsIdentifier('toLowerCase)) with JsStringType

    def toUpperCase = new JsMember(this, JsIdentifier('toUpperCase)) with JsStringType
  }

  trait JsRegExpType extends JsObjectType {
    self: JsExpression ⇒

    def global = new JsMember(this, JsIdentifier('global)) with JsBooleanType

    def ignoreCase = new JsMember(this, JsIdentifier('ignoreCase)) with JsBooleanType

    def lastIndex = new JsMember(this, JsIdentifier('lastIndex)) with JsNumberType

    def multiline = new JsMember(this, JsIdentifier('multiline)) with JsBooleanType

    def source = new JsMember(this, JsIdentifier('source)) with JsStringType

    def compile = new JsMember(this, JsIdentifier('compile))

    def exec = new JsMember(this, JsIdentifier('exec)) with JsArrayType

    def test = new JsMember(this, JsIdentifier('test)) with JsBooleanType
  }

  // Browser Objects

  trait JsDOMWindowType extends JsObjectType {
    self: JsExpression ⇒

    def closed = new JsMember(this, JsIdentifier('closed)) with JsBooleanType

    def defaultStatus = new JsMember(this, JsIdentifier('defaultStatus)) with JsStringType

    def document = new JsMember(this, JsIdentifier('document)) //TODO

    def frames = new JsMember(this, JsIdentifier('frames)) with JsArrayType

    def history = new JsMember(this, JsIdentifier('history)) //TODO

    def innerHeight = new JsMember(this, JsIdentifier('innerHeight)) with JsNumberType

    def innerWidth = new JsMember(this, JsIdentifier('innerWidth)) with JsNumberType

    def length = new JsMember(this, JsIdentifier('length)) with JsNumberType

    def location = new JsMember(this, JsIdentifier('location)) //TODO

    def jsName = new JsMember(this, JsIdentifier('name)) with JsStringType

    def navigator = new JsMember(this, JsIdentifier('navigator)) //TODO

    def opener = new JsMember(this, JsIdentifier('opener)) with JsDOMWindowType

    def outerHeight = new JsMember(this, JsIdentifier('outerHeight)) with JsNumberType

    def outerWidth = new JsMember(this, JsIdentifier('outerWidth)) with JsNumberType

    def pageXOffset = new JsMember(this, JsIdentifier('pageXOffset)) with JsNumberType

    def pageYOffset = new JsMember(this, JsIdentifier('pageYOffset)) with JsNumberType

    def parent = new JsMember(this, JsIdentifier('parent)) with JsDOMWindowType

    def screen = new JsMember(this, JsIdentifier('screen)) //TODO

    def screenLeft = new JsMember(this, JsIdentifier('screenLeft)) with JsNumberType

    def screenTop = new JsMember(this, JsIdentifier('screenTop)) with JsNumberType

    def screenX = new JsMember(this, JsIdentifier('screenX)) with JsNumberType

    def screenY = new JsMember(this, JsIdentifier('screenY)) with JsNumberType

    def jsSelf = new JsMember(this, JsIdentifier('self)) with JsDOMWindowType

    def status = new JsMember(this, JsIdentifier('status)) with JsStringType

    def top = new JsMember(this, JsIdentifier('top)) with JsDOMWindowType
    
    def alert = new JsMember(this, JsIdentifier('alert))

    def blur = new JsMember(this, JsIdentifier('blur))

    def clearInterval = new JsMember(this, JsIdentifier('clearInterval))

    def clearTimeout = new JsMember(this, JsIdentifier('clearTimeout))

    def close = new JsMember(this, JsIdentifier('close))

    def confirm = new JsMember(this, JsIdentifier('confirm)) with JsBooleanType

    def focus = new JsMember(this, JsIdentifier('focus))

    def moveBy = new JsMember(this, JsIdentifier('moveBy))

    def moveTo = new JsMember(this, JsIdentifier('moveTo))

    def open = new JsMember(this, JsIdentifier('open)) with JsDOMWindowType

    def print = new JsMember(this, JsIdentifier('print))

    def prompt = new JsMember(this, JsIdentifier('prompt)) with JsStringType

    def resizeBy = new JsMember(this, JsIdentifier('resizeBy))

    def resizeTo = new JsMember(this, JsIdentifier('resizeTo))

    def scroll = new JsMember(this, JsIdentifier('scroll))

    def scrollBy = new JsMember(this, JsIdentifier('scrollBy))

    def scrollTo = new JsMember(this, JsIdentifier('scrollTo))

    def setInterval = new JsMember(this, JsIdentifier('setInterval)) with JsNumberType

    def setTimeout = new JsMember(this, JsIdentifier('setTimeout)) with JsNumberType
  }
}
