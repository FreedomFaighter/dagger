package coffee;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;

@Module(
    injects = CoffeeApp.class,
    includes = PumpModule.class
)
class DripCoffeeModule {
  @Provides @Singleton Heater provideHeater() {
    return new ElectricHeater();
  }
}
