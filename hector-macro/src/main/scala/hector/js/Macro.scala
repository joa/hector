package hector.js

object Macro {
  import language.experimental.macros

  import scala.reflect.macros.Context


  private[this] val Binops = Set(
    "==",
    "!=",
    "<",
    "<=",
    ">",
    ">=",
    "<<",
    ">>",
    ">>>",
    "+",
    "-",
    "*",
    "/",
    "%",
    "|",
    "^",
    "&",
    "||"
  )
  
  private[this] val NameMapping = Map(
    "jsWindow" -> "window"
  )

  private[this] class JSCompiler[C <: Context](val c: C) {
    import c.universe._

    def compile(scalaAST: Tree): c.Expr[JsAST] = {
      println(showRaw(scalaAST))
      val res = transform(scalaAST)


      //println(showRaw(res.tree))
      //println("########")
      //println(res.tree)
      //println(showRaw(res))
      res
    }

    private[this] val toExpr: PartialFunction[Tree, c.Expr[JsExpression]] = {
      case Function(parameters, body) ⇒ 
        val jsParams = 
          parameters collect {
            case ValDef(_, name, _, _) ⇒ toIdent(name)
          }

        reify {
          JsFunc(None, listToExpr(jsParams).splice, JsBlock(Seq(toStmt(body).splice)))
        } 

      case Apply(Select(lhs, opTerm @ Name(opName)), rhs :: Nil) if Binops.contains(opName) ⇒
        val opTree = 
          c.Expr[JsBinop](
            Select(Select(Select(Ident(newTermName("hector")), newTermName("js")), newTermName("JsBinops")), opTerm)
          )

        reify {
          JsBinary(toExpr(lhs).splice, opTree.splice, toExpr(rhs).splice)
        }

      case Apply(Select(obj, Name("selectDynamic")), List(Literal(Constant(member: String)))) ⇒
        reify {
          JsMember(toExpr(obj).splice, JsIdentifier(Symbol(c.literal(member).splice)))
        }

      case Apply(Apply(Select(obj, Name("updateDynamic")), List(Literal(Constant(member: String)))), value :: Nil) ⇒
        reify {
          JsBinary(JsMember(toExpr(obj).splice, JsIdentifier(Symbol(c.literal(member).splice))), JsBinops.`=`, toExpr(value).splice)
        }

      case Apply(callee, args) ⇒
        reify {
          JsCall(toExpr(callee).splice, toSeqOfExpr(args).splice)
        }

      case TypeApply(select, _) ⇒
        toExpr(select)

      case Select(Select(Select(Ident(Name("hector")), Name("js")), Name("Macro")), Name("self")) =>
        reify {
          JsThis
        }

      case Select(Select(Select(Ident(Name("hector")), Name("js")), Name("toplevel")), Name(x)) if NameMapping.contains(x) =>
        reify {
          JsIdentifier(Symbol(c.literal(NameMapping(x)).splice))
        }


      case Select(expr, term) ⇒
        reify {
          JsMember(toExpr(expr).splice, toIdent(term).splice)
        }

      case Assign(select, expr) ⇒
        reify {
          JsBinary(toExpr(select).splice, JsBinops.`=`, toExpr(expr).splice)
        }

      case Ident(name) ⇒
        toIdent(name)

      case Literal(Constant(x: Byte)) ⇒ 
        reify {
          JsNumber[Byte](c.literal(x).splice)
        }

      case Literal(Constant(x: Short)) ⇒ 
        reify {
          JsNumber[Short](c.literal(x).splice)
        }

      case Literal(Constant(x: Int)) ⇒ 
        reify {
          JsNumber[Int](c.literal(x).splice)
        }

      case Literal(Constant(x: Long)) ⇒ 
        reify {
          JsNumber[Long](c.literal(x).splice)
        }

      case Literal(Constant(x: Float)) ⇒ 
        reify {
          JsNumber[Float](c.literal(x).splice)
        }

      case Literal(Constant(x: Double)) ⇒ 
        reify {
          JsNumber[Double](c.literal(x).splice)
        }

      case Literal(Constant(true)) ⇒ 
        reify {
          JsTrue
        }

      case Literal(Constant(false)) ⇒ 
        reify {
          JsFalse
        }

      case Literal(Constant(x: String)) ⇒
        reify {
          JsString(c.literal(x).splice)
        }

      case Literal(Constant(null)) ⇒
        reify {
          JsNull
        }

      case Literal(Constant(())) ⇒
        reify {
          JsIdentifier('undefined)
        }

      case This() ⇒
        reify {
          JsThis
        }

      // Note that in Scala even if or {} is an expression
      // returning a value. We do not perform this mapping
      // at the moment since it would mean we have to
      // perform transformBlockToExpr for nearly every
      // statement we encounter here.
      // This is not feasable until we have an optimization
      // phase that would eliminate unnecessary functions.
      //
      // Imagine the following Scala code:
      //
      //
      //  var x = 
      //    if(cond) {
      //      x = 1
      //    } else {
      //      x = 2
      //    }
      //  return
      //    if(x == 1) {
      //      console.log("not so trivial")
      //      "foo"
      //    } else {
      //      "bar"
      //    }
      //  
      //
      // It would result in code similar to this
      //
      // (function(){
      //   var x = 
      //     (cond) ?
      //       (function(){
      //         return 1
      //       })()
      //     :
      //       (function(){
      //         return 2
      //       })();
      //   return
      //     (x == 1) ?
      //       (function(){
      //         console.log("not so trivial")
      //         return "foo"
      //       })()
      //     :
      //       (function(){
      //         return "bar"
      //       })();
      // })();
      //
      // Since this is not acceptable, we are not going to implement
      // the block-is-an-expression idiom at the moment.
      // Of course an ideal output (without other optimization applied)
      // would be
      //
      // var x = cond ? 1 : 2
      // return x == 1 ? (function(){console.log("not so trivial"); return "foo"})() : "bar"
      //  
    }

    private[this] val toStmt: PartialFunction[Tree, c.Expr[JsStatement]] = {
      case Apply(Select(Select(Select(Ident(Name("hector")), Name("js")), Name("Macro")), Name("ret")), args) ⇒
        args match {
          case Nil ⇒ reify { JsReturn(value = None) }
          case value :: Nil ⇒ reify { JsReturn(Some(toExpr(value).splice)) }
          case x :: xs ⇒ sys.error("Cannot call hector.js.Macro.ret with more than one argument")
        }

      case Block(stmts, expr) ⇒
        val allStmts = 
          if(isUnit(expr)) {
            stmts
          } else {
            stmts :+ expr
          }

        reify {
          JsBlock(toSeqOfStmt(allStmts).splice)
        }

      case ValDef(_, name, _, init) ⇒
        reify {
          JsVar(toIdent(name).splice, Some(toExpr(init).splice))
        }

      case If(cond, thenPart, elsePart) ⇒
        reify {
          JsIf(toExpr(cond).splice, toStmt(thenPart).splice, optionToExpr(unitAsNone(elsePart) map toStmt).splice)
        }

      case Return(Block(stmts, expr)) ⇒
        if(stmts.isEmpty && isUnit(expr)) {
          reify {
            JsReturn(None)
          }
        } else {
          reify {
            JsReturn(Some(transformBlockToExpr(stmts, expr).splice))
          }
        }

      case expr ⇒ 
        reify {
          JsExpStatement(toExpr(expr).splice)
        }

    }

    private def listToExpr[T](exprs: List[Expr[T]]): Expr[Seq[T]] = 
      c.Expr[List[T]](treeBuild.mkMethodCall(reify(List).tree, exprs.map(_.tree)))

    private def optionToExpr[T](expr: Option[Expr[T]]): Expr[Option[T]] = 
      expr match {
        case Some(value) ⇒
          c.Expr[Option[T]](treeBuild.mkMethodCall(reify(Option).tree, List(value.tree)))
        case None ⇒
          reify(None)
      }

    private[this] def toSeqOfStmt(seq: Seq[Tree]): c.Expr[Seq[JsStatement]] = listToExpr(seq.to[List] map toStmt)

    private[this] def toSeqOfExpr(seq: Seq[Tree]): c.Expr[Seq[JsExpression]] = listToExpr(seq.to[List] map toExpr)

    private[this] def toIdent(name: Name): c.Expr[JsIdentifier] =
      reify {
        JsIdentifier(Symbol(c.literal(name.decoded).splice))
      }

    private[this] def unitAsNone(expr: Tree): Option[Tree] =
      if(isUnit(expr)) {
        None
      } else {
        Some(expr)
      }

    private[this] def isUnit(expr: Tree): Boolean =
      expr.equalsStructure(c.literalUnit.tree)

    private[this] def transformBlockToExpr(stmts: Seq[Tree], expr: Tree): c.Expr[JsExpression] =
      if(stmts.isEmpty) {
        if(isUnit(expr)) {
          // We expect an expression from a Scala block that results in Unit
          // like def foo(): Unit = return () but in JS this would be encoded
          // as something like (function(){return;})() which results in undefined
          reify {
            JsIdentifier('undefined)
          }
        } else {
          // Got a single expression that we need to return and it won't be Unit
          if(toExpr.isDefinedAt(expr)) {
            toExpr(expr)
          } else {
            // The actual return statment might be a block as well, so we
            // try to figure out if its inner statement can be transformed
            // with a trivial case or if we need to shuffle it and treat
            // it as a sequence of statements we are going to return
            expr match {
              case Block(innerStmts, innerExpr) if innerStmts.length == 1 && isUnit(innerExpr) ⇒
                // Block with a single statement. Try this one first.
                transformBlockToExpr(innerStmts, innerExpr)

              case arbitraryTree ⇒
                transformBlockToExpr(Seq(expr), reify(()).tree)
            }
          }
        }
      } else if(stmts.length == 1 && isUnit(expr)) {
        // Got a single statement which is usually a block with a single
        // statement inside. If that is the case we will simply go one
        // level deeper
        stmts.head match {
          case Block(innerStmts, innerExpr) if innerStmts.length == 1 && isUnit(innerExpr) ⇒
            // Block with a single statement. Try this one first.
            transformBlockToExpr(innerStmts, innerExpr)

          case arbitraryTree ⇒
            if(toExpr.isDefinedAt(arbitraryTree)) {
              toExpr(arbitraryTree)
            } else {
              // We have an issue since there is only one statement available and it is not 
              // an expression -- undefined it is.
              reify {
                JsCall(JsFunc(None, Seq.empty, JsBlock(Seq(toStmt(arbitraryTree).splice))), Seq.empty)
              }
            }
        }
      } else {
        // If the result is Unit we do not have to return anything.
        // If however there is an expression we need to transform
        // it to an expression again and create a JsReturn statment
        //
        // TODO(joa): if resultOption is None we should also
        // wrap toSeqOfStmt
        val resultOption = 
          optionToExpr(
            unitAsNone(expr) map { 
              x => transformBlockToExpr(Seq.empty, x)
            } map {
              x => reify { JsReturn(Some(x.splice)) }
            }
          )

        reify {
          JsCall(JsFunc(None, Seq.empty, JsBlock(toSeqOfStmt(stmts).splice ++ resultOption.splice.toList)), Seq.empty)
        }
      }

    object Name {
      def unapply(name: Name) = Some(name.decoded)
    }

    private[this] def transform(scalaAST: Tree): c.Expr[JsAST] =
      (toExpr orElse toStmt)(scalaAST)
  }

  def js(expr: Any): JsAST = macro convertScalaASTtoJsAST

  def convertScalaASTtoJsAST(c: Context)(expr: c.Expr[Any]): c.Expr[JsAST] = {
    val jsCompiler = new JSCompiler[c.type](c)
    jsCompiler.compile(expr.tree)
  }


  // JavaScript control structures, not tied to Scala's
  // structures in order to have more flexibility when
  // it comes to semantics

  //TODO(joa): can we make this more secure with the magic of an implicit?
  def ret(expr: Any) = throw new IllegalStateException("ret should not be used outside of macro context")
  def ret(): Unit = throw new IllegalStateException("ret should not be used outside of macro context")
  val self = hector.js.JsThis
}
