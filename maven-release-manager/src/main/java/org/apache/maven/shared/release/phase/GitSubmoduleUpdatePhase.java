package org.apache.maven.shared.release.phase;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.apache.maven.shared.release.exec.CommandLineFactory;
import org.apache.maven.shared.release.exec.ForkedMavenExecutor;
import org.apache.maven.shared.release.exec.TeeOutputStream;
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.cli.Commandline;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.apache.maven.shared.utils.logging.MessageUtils.buffer;

/**
 * @author <a href="mailto:wuwen.55@aliyun.com">Wen wu</a>
 */
@Singleton
@Named( "submodule-update" )
public class GitSubmoduleUpdatePhase
        extends AbstractReleasePhase
{
    /**
     * Tool that gets a configured SCM repository from release configuration.
     */
    private final CommandLineFactory commandLineFactory;

    @Inject
    public GitSubmoduleUpdatePhase( CommandLineFactory commandLineFactory )
    {
        this.commandLineFactory = requireNonNull( commandLineFactory );
    }

    @Override
    public ReleaseResult execute( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                                  List<MavenProject> reactorProjects )
            throws ReleaseExecutionException, ReleaseFailureException
    {
        ReleaseResult releaseResult = new ReleaseResult();
        String additionalArguments = releaseDescriptor.getAdditionalArguments();
        logInfo( releaseResult, "custom exec " + additionalArguments );

        try 
        {
            File workingDirectory = new File( releaseDescriptor.getCheckoutDirectory() );

            File executionRoot;
            if ( releaseDescriptor.getPomFileName() != null )
            {
                File rootPom = new File( workingDirectory, releaseDescriptor.getPomFileName() );
                executionRoot = rootPom.getParentFile();
            }
            else
            {
                executionRoot = workingDirectory;
            }
            
            if ( additionalArguments != null && additionalArguments.contains( "-DsubmoduleUpdate" ) )
            {
                logInfo( releaseResult, "custom exec git submodule update" );
                Commandline commandLine = commandLineFactory.createCommandLine( "git" );
                commandLine.setWorkingDirectory( executionRoot.getAbsolutePath() );
                commandLine.createArg().setValue( "submodule" );
                commandLine.createArg().setValue( "update" );
                commandLine.createArg().setValue( "--init" );
                commandLine.createArg().setValue( "--recursive" );

                getLogger().info( "Executing: " + commandLine );

                TeeOutputStream stdOut = new TeeOutputStream( System.out );

                TeeOutputStream stdErr = new TeeOutputStream( System.err );

                int i = ForkedMavenExecutor.executeCommandLine( commandLine, System.in, stdOut, stdErr );
                logInfo( releaseResult, "custom exec git submodule update result " + i );
            }
        } 
        catch ( Exception e ) 
        {
            logWarn( releaseResult, e.getMessage() );
        }

        releaseResult.setResultCode( ReleaseResult.SUCCESS );
        return releaseResult;
    }

    @Override
    public ReleaseResult simulate( ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
                                   List<MavenProject> reactorProjects )
            throws ReleaseExecutionException, ReleaseFailureException
    {
        ReleaseResult result = new ReleaseResult();

        MavenProject rootProject = ReleaseUtil.getRootProject( reactorProjects );
        File checkoutDirectory =
                        FileUtils.resolveFile( rootProject.getBasedir(), releaseDescriptor.getCheckoutDirectory() );

        if ( releaseDescriptor.isLocalCheckout() )
        {
            logInfo( result,
                     "The project would have a " + buffer().strong( "local" )
                         + " check out to perform the release from " + checkoutDirectory + "..." );
        }
        else
        {
            logInfo( result,
                     "The project would be checked out to perform the release from " + checkoutDirectory + "..." );
        }

        result.setResultCode( ReleaseResult.SUCCESS );
        return result;
    }
}
