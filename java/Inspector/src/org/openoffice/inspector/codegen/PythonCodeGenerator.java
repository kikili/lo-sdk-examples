/*************************************************************************
 *
 *  The Contents of this file are made available subject to the terms of
 *  the BSD license.
 *  
 *  Copyright (c) 2009 by Sun Microsystems, Inc.
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *  1. Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *  3. Neither the name of Sun Microsystems, Inc. nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *  FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *  COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *  BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 *  OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 *  TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 *  USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *     
 *************************************************************************/

package org.openoffice.inspector.codegen;

import com.sun.star.reflection.XIdlMethod;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.openoffice.inspector.util.Resource;
import org.openoffice.inspector.util.StringTemplate;

/**
 * CodeGenerator for Python source code.
 * @author Christian Lins (cli@openoffice.org)
 */
public class PythonCodeGenerator extends CodeGenerator
{
 
  private Set<String> properties      = new HashSet<String>();
  private Set<String> queryInterfaces = new HashSet<String>();
  private List<XIdlMethod> invokeMethods = new ArrayList<XIdlMethod>();
  
  private StringTemplate tmplProgram = new StringTemplate(
    Resource.getAsString("org/openoffice/inspector/codegen/template/PythonProgramStub.tmpl"));
  
  @Override
  public void addAccessorCodeFor(String property)
  {
    if(!this.properties.contains(property))
      this.properties.add(property);
  }
  
  @Override
  public void addInvokeCodeFor(XIdlMethod method)
  {
    if(!this.invokeMethods.contains(method))
      this.invokeMethods.add(method);
  }
  
  @Override
  public void addQueryCodeFor(String iface)
  {
    if(!this.queryInterfaces.contains(iface))
      this.queryInterfaces.add(iface);
  }
  
  @Override
  public String getSourceCode()
  {
    return this.tmplProgram.toString();
  }
  
  @Override
  public Language getLanguage()
  {
    return Language.Python;
  }
  
}
