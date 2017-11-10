<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<div class="heading">
  <a href="javascript:toggleDiv('entityDiv');">
    <img id='entityDivToggle' src="images/disclosed.gif"/> Custom tile heading
  </a>
</div>

<div id="entityDiv" style="display:block;" class="body">
  <p>Custom tile body.</p>
  <ol>
    <li>
      <im:querylink text="Execute employeesWithOldManagers, skipping query builder" skipBuilder="true">
        <query name="employeesWithOldManagers" model="testmodel" view="Employee.name Employee.age Employee.department.name Employee.department.manager.age">
          <node path="Employee.department.manager.age">
            <constraint op=">" value="10"/>
          </node>
        </query>
      </im:querylink>
    </li>
    <li>
      <im:querylink text="Execute employeesWithOldManagers, go to builder" skipBuilder="false">
        <query name="employeesWithOldManagers" model="testmodel" view="Employee.name Employee.age Employee.department.name Employee.department.manager.age">
          <node path="Employee.department.manager.age">
            <constraint op=">" value="10"/>
          </node>
        </query>
      </im:querylink>
    </li>
  </ol>
</div>
