


func makeIosGlobalScope() -> Scope {
  let scope = Scope(makeGlobalScope())
  
  return scope
}

let globalIosScope = makeIosGlobalScope()
