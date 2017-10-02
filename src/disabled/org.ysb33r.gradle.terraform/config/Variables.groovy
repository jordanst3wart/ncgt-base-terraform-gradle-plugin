package org.ysb33r.gradle.terraform.config

import groovy.transform.CompileStatic
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.ysb33r.grolifant.api.MapUtils
import org.ysb33r.grolifant.api.StringUtils
import org.ysb33r.gradle.terraform.tasks.base.AbstractTerraformTask

/** A configuration building block for tasks that need to pass variables to
 * a {@code terraform task}.
 *
 * >p> To add this to a task
 *
 * <code>
 * void variables(@DelegatesTo(Variables) Closure cfg) {
 *   configureNested(this.variables,cfg)
 * }
 *
 * void variables(Action<Variables> a) {
 *   a.execute(this.variables)
 * }
 *
 * // At construction time, initialise this with a reference to
 * // the task it is attached to.
 * @Nested
 * final Variables variables
 * </code>
 *
 * @since 0.1
 */
@CompileStatic
class Variables {

    /** Attached this configuration block to a task at construction time.
     *
     * @param task Associated task
     */
    Variables(AbstractTerraformTask task) {
        this.task = task
    }

    /** Adds one variable.
     *
     * <p> This will replace any previous entry by the same name.
     *
     * @param name Name of variable.
     * @param value Lazy-evaluated form of variable. Anything resolvable via {@link org.ysb33r.grolifant.api.StringUtils.stringsize(Object)}
     * is accepted.
     */
    void var(final String name,final Object value) {
        vars.put(name,value)
    }

    /** Adds a map as a variable.
     *
     * <p> This will replace any previous entry by the same name.
     *
     * @param name Name of variable.
     * @param val1 First
     * @param vals Lazy-evaluated forms of variable. Anything resolvable via {@link org.ysb33r.grolifant.api.MapUtils.stringsizeValues(Map<String,Object>)}
     * is accepted.
     */
    void map(Map<String,?> map,final String name) {
        vars.put(name,map)
    }

    /** Adds a list as a variable.
     *
     * <p> This will replace any previous entry by the same name.
     *
     * @param name Name of variable.
     * @param val1 First
     * @param vals Lazy-evaluated forms of variable. Anything resolvable via {@link org.ysb33r.grolifant.api.StringUtils.stringize(Iterable<?>)}
     * is accepted.
     */
    void list(final String name,Object val1,Object... vals) {
        List<Object> inputs = [val1]
        inputs.addAll(vals)
        vars.put(name,inputs)
    }

    /** Adds a list as a variable.
     *
     * <p> This will replace any previous entry by the same name.
     *
     * @param name Name of variable.
     * @param vals Lazy-evaluated forms of variable. Anything resolvable via {@link org.ysb33r.grolifant.api.StringUtils.stringize(Iterable<?>)}
     * is accepted.
     */
    void list(final String name,Iterable<?> vals) {
        vars.put(name,vals as List)
    }

    /** Add a file containing {@code terraform} variables.
     *
     * @param fileName File that can be resolved via {@code project.file}.
     */
    void file(final Object fileName) {
        files.add fileName
    }

    /** Evaluate all variables and convert them to Terraform-compliant strings, ready to be passed to command-line.
     *
     * <p> Calling this will resolve all lazy-evaluated entries.
     *
     * @return Map where each key is the name of a variable. Each value is correctly formatted according to the kind of variable.
     */
    @Input
    Map<String,String> getVars() {
        Map<String,String> hclMap = [:]
        for( String key in this.vars.keySet() ) {
            Object var = vars[key]
            switch(var) {
                case Map:
                    String joinedMap = MapUtils.stringizeValues((Map)var).collect { String k, String v ->
                        "\"${k}\" : \"${v}\"".toString()
                    }.join(', ')
                    hclMap[key] = "{${joinedMap}}".toString()
                    break
                case List:
                    String joinedList = StringUtils.stringize((Iterable)var).collect { "\"${it}\"".toString() }.join(', ')
                    hclMap[key] = "[${joinedList}]".toString()
                    break
                default:
                    hclMap[key] = StringUtils.stringize(var)
            }
        }
        return hclMap
    }

    /** List of files containing Terraform variables.
     *
     * @return List of files.
     */
    @InputFiles
    FileCollection getFiles() {
        task.project.files(this.files)
    }

    final private AbstractTerraformTask task
    private Map<String,Object> vars = [:]
    private List<Object> files = []
}
