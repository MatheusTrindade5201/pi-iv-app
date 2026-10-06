package br.univesp.pi.expedientes.feature.profile

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ProfileModule {
    @Binds
    abstract fun bindProfileRepository(impl: DefaultProfileRepository): ProfileRepository
}
